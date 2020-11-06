module rrprint; % Output interface for Run-REDUCE (a JavaFX GUI for REDUCE)

% This file is a version of "tmprint.red" modified by Francis Wright.
% It outputs algebraic-mode mathematics using LaTeX-like markup.

% I (FJW) think the history of this file is as follows, but please see
% "fmprint.red" and "tmprint.red" for further details.  It began life
% as "fmprint.red" by Herbert Melenk, using ideas from "maprin.red"
% (by Anthony Hearn and Arthur Norman), which produced TeX-like output
% to drive a REDUCE GUI for an early version of Microsoft Windows and
% made some direct references to characters in the Microsoft Windows
% Symbol font.

% It was then developed into "tmprint.red" by Andrey Grozin and
% several other authors (see below) to drive the TeXmacs GUI and
% developed further by Arthur Norman to drive the CSL REDUCE GUI.  I
% now propose to use it to drive my own GUI, so I will remove the code
% specific to TeXmacs and CSL whilst aiming not to break the LaTeX
% output!

% The code at the end of this file is based on code from
% "redfront.red" and supports colouring of non-typeset algebraic-mode
% mathematical output.

% Francis Wright, initiated September 2020.

% ----------------------------------------------------------------------
% $Id: tmprint.red 5408 2020-09-25 12:22:46Z eschruefer $
% ----------------------------------------------------------------------
% Copyright (c) 1993-1994, 1999, 2003-2005 A. Dolzmann, T. Hearn, A.
% Grozin, H. Melenk, W. Neun, A. Norman, A. Seidl, and T. Sturm
%
% Permission is hereby granted, free of charge, to any person
% obtaining a copy of this software and associated documentation files
% (the "Software"), to deal in the Software without restriction,
% including without limitation the rights to use, copy, modify, merge,
% publish, distribute, sublicense, and/or sell copies of the Software,
% and to permit persons to whom the Software is furnished to do so,
% subject to the following conditions:
%
% The above copyright notice and this permission notice shall be
% included in all copies or substantial portions of the Software.
%
% THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
% EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
% MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
% NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
% BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
% ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
% CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
% SOFTWARE.
% ----------------------------------------------------------------------

% Switches:
%
%  on fancy                enable algebraic output processing.
%                          (Defaults to on when the package is loaded.)
%
% Properties:
%
%  fancy!-prifn            print function for an operator.
%
%  fancy!-pprifn           print function for an operator including current
%                          operator precedence for infix printing.
%
%  fancy!-flatprifn        print function for objects which require
%                          special printing if prefix operator form
%                          would have been used, e.g. matrix, list.
%
%  fancy!-prtch            string for infix printing of an operator
%
%  fancy!-special!-symbol  print expression for a non-indexed item
%                          string with TeX expression e.g. "\alpha " or a
%                          number referring to ASCII symbol code (deprecated).
%
%  fancy!-infix!-symbol    special symbol for an infix operator.
%
%  fancy!-functionsymbol   special symbol for a (prefix) function.
%
%  fancy!-symbol!-length   the number of horizontal units needed for
%                          a special symbol.  A standard character has
%                          2 units, which is the default.

% To enable typeset algebraic-mode output set
% outputhandler!* := 'fancy!-output;

create!-package('(rrprint), nil);

fluid  '(
         !*list
         !*nat
         !*nosplit
         !*ratpri
         !*revpri
         curline!*
         overflowed!*
         p!*!*
         testing!-width!*
         tablevel!*
         sumlevel!*
         outputhandler!*
         outputhandler!-stack!*
         posn!*
         long!*
         obrkp!*    % outside-brackets-p
            );

global '(ofl!*);

%
% The interaction between the code here and a variety of other REDUCE flags
% that set output options is possibly delicate and probably often broken.
% As well as "list" the code here needs review with regard to options
% such as "fort" for generating other formats of output.
%

switch list,ratpri,revpri,nosplit;

% Temp experiment while investigating a possible fix for an interaction with
% "on list". Well in fact "on/off acn" can provide a general guard for
% some incremental changes being made here.   But evenually this switch
% will be retired.                 ACN March 2011
switch acn;
on acn;                                 % FJW Seems better!

% Global variables initialized in this section.

fluid '(
      fancy!-switch!-on!*
      fancy!-switch!-off!*
      !*fancy!-mode
      fancy!-pos!*
      fancy!-line!*
      fancy!-page!*
      fancy!-bstack!*
      !*fancy!-lower    % control of conversion to lower case
      );

fluid '(fancy!-texpos);                 %FJW not really used?

fancy!-switch!-on!* := int2id 16$
fancy!-switch!-off!* := int2id 17$
!*fancy!-lower := nil;

% global '(fancy_lower_digits);  % not currently used

% share fancy_lower_digits; % T, NIL or ALL.

% if null fancy_lower_digits then fancy_lower_digits:=t;

global '(fancy_print_df);

share fancy_print_df;     % PARTIAL, TOTAL, INDEXED.

if null fancy_print_df then fancy_print_df := 'partial;
switch fancy;

% put('fancy,'simpfg,
%   '((t (fmp!-switch t))
%     (nil (fmp!-switch nil)) ));

% symbolic procedure fmp!-switch mode;
%       if mode then
%         <<if outputhandler!* neq 'fancy!-output then
%           <<outputhandler!-stack!* :=
%                 outputhandler!* . outputhandler!-stack!*;
%            outputhandler!* := 'fancy!-output;
%           >>;
%           % !*fancy := t %FJW Handled by switch module
%         >>
%       else
%         <<if outputhandler!* = 'fancy!-output then
%           <<outputhandler!* := car outputhandler!-stack!*;
%             outputhandler!-stack!* := cdr outputhandler!-stack!*;
%             % !*fancy := nil %FJW Handled by switch module
%           >>
% 	  else
%           << % !*fancy := nil; %FJW Handled by switch module
%              rederr "FANCY is not current output handler" >>
% % ACN feels that raising an error on an attempt to switch off an option
% % in the case that the option is already disabled is a bit harsh.
%         >>;

% fluid '(lispsystem!*);

symbolic procedure fancy!-out!-item(it);
   % Called by fancy!-flush only.
  if atom it then prin2 it else
  if eqcar(it,'ascii) then prin2 int2id cadr it else
  if eqcar(it,'tab) then
      for i:=1:cdr it do prin2 " "
    else
  if eqcar(it,'bkt) then
%     begin scalar m,b,l; integer n;
%      m:=cadr it; b:=caddr it; n:=cadddr it;
     begin scalar b, l;
      b := caddr it;
      l := b member '( !( !{ );
   %  if m then prin2 if l then "\left" else "\right"
   % else
%      if n> 0 then
%      <<prin2 if n=1 then "\big" else if n=2 then "\Big" else
%            if n=3 then "\bigg" else "\Bigg";
%       prin2 if l then "l" else "r";
%      >>;
      if l then prin2 "\left" else prin2 "\right";
      if b member '(!{ !}) then prin2 "\";
      prin2 b;
    end
  else <<
     % Finish processing as fancy!-flush() would have done to avoid
     % hanging the GUI:
     fancy!-out!-trailer();
     set!-fancymode nil;
     rederr {"unknown print item", it};
  >>;

symbolic procedure set!-fancymode bool;
  if bool neq !*fancy!-mode then
    <<!*fancy!-mode:=bool;
      fancy!-pos!* :=0;
      fancy!-page!*:=nil;
      fancy!-line!*:=nil;
      overflowed!* := nil;
        % new: with tab
      fancy!-line!*:= '((tab . 1));
      fancy!-pos!* := 10;
      sumlevel!* := tablevel!* := 1;
   >>;

symbolic procedure fancy!-output(mode,l);
% Interface routine.
%
% ACN does not understand the "posn!*>2" filter here. To avoid some
% bad consequences it was having for my new screen/log-file stuff it now only
% applies in maprin mode not terpri mode, but it would be nice if somebody
% could explain to me just why it was needed in the first case at all.  I can
% imagine that if "on fancy" is acticated when there is still some partly-
% printed expression (in non-fancy mode) buffered up the terpri!* to flush it
% may need special care. But if that is what it is about I would suggest that
% treatment be applied in fmp!-switch not here...
%
   if ofl!* or (mode='maprin and posn!*>2) or not !*nat then <<
% not terminal handler or current output line non-empty.
      if mode = 'maprin then maprin l
      else terpri!*(l) >> where outputhandler!* = nil
      else
   <<set!-fancymode t;
      if mode = 'maprin then
         fancy!-maprin0 l
      else if mode = 'assgnpri then <<
         fancy!-assgnpri l;
         fancy!-flush() >>
      else
         fancy!-flush() >>;

% fancy!-assgnpri checks whether a special printing function is defined
% and calls it
symbolic procedure fancy!-assgnpri u;
   % E.g. a := b := c := mat(()) -> u = ((mat (0)) (a b c) only)
   % FJW But this seems to be called when there is no assignment! Why?
   begin scalar x,y;
     x := getrtype car u;               % e.g. matrix; tag = mat
     y := get(get(x,'tag),'fancy!-assgnpri);
     return if y then apply1(y,u) else fancy!-maprin0 car u
   end;

symbolic procedure fancy!-assgnpri!-matrix u; % FJW
   % E.g. a := b := c := mat(()) -> u = ((mat (0)) (a b c) only)
   % Plain printing displays this as "a := [0]", ignoring b and c!
   begin scalar lhvars := cadr u;
      if lhvars then <<
         % if cdr lhvars then
         %    fancy!-inprint('setq, get('setq,'infix), lhvars)
         % else
            fancy!-maprin0 car lhvars;
         fancy!-oprin 'setq;
      >>;
      return fancy!-maprin0 car u
   end;

put('mat, 'fancy!-assgnpri, 'fancy!-assgnpri!-matrix); % FJW

symbolic procedure fancy!-out!-header();
   <<
      if posn()>0 then terpri();
      prin2 fancy!-switch!-on!*;
   >>;

symbolic procedure fancy!-out!-trailer();
   prin2 fancy!-switch!-off!*;

symbolic procedure fancy!-flush();
   %FJW Modified to avoid leading spaces and precede a leading + or -
   % on a follow-on line with an invisible term using an empty text
   % box (see the LaTeX book, page 48, but KaTeX does not support \mbox).
   (begin scalar not_first_line;
      fancy!-terpri!* t;
      for each line in reverse fancy!-page!* do
         if line and not eqcar(car line,'tab) then <<
            fancy!-out!-header();
            % for each it in reverse line do fancy!-out!-item it;
            line := reverse line;
            while eqcar(car line, 'tab) do line := cdr line;
            if not_first_line and car line memq '(!+ !-) then fancy!-out!-item "\mathrm{}";
            for each it in line do fancy!-out!-item it;
            fancy!-out!-trailer();
            not_first_line := t
         >>;
      set!-fancymode nil;
   end) where !*lower = nil;

%---------------- primitives -----------------------------------

symbolic procedure fancy!-special!-symbol(u,n);
   if numberp u then
     <<fancy!-prin2!*("\symb{",n);
       fancy!-prin2!*(u,0);
       fancy!-prin2!*("}",0);
     >>
    else fancy!-prin2!*(u,n);

symbolic procedure fancy!-prin2 u;
    fancy!-prin2!*(u,nil);

% fancy!-prin2!* maintains a variable fancy!-pos!* which is compared
% against (multiples of) linelength. This is not incremented when a
% TeX keyword is inserted. That is probably reasonable for some
% words such as "\mathrm", but seems odd for "\alpha".
% It is incremented for "{" and "}" and also for "^" and "_". That also
% seems deeply wrong. And to the extent that it is used to estimate the
% width of the current part-line it is certainly oblivious to the
% different metrics that "\,", "i", "m", and "\ldots" might have, where
% those are rather more than minor.
%
% So even if one assumes that the units in which linelength() returns
% its value are relevant in TeX output (they probably are at least
% roughly, except that the idea of users altering linelength and getting
% different behaviour seems pretty scary to me, and the potential confusion
% between desired width of mathematical display and the number of character
% positions that the TeX material should fit with seems messy) the calculation
% done here is a bit of a mess.

% for each x in '(
%     !\sqrt        !\equiv        !\alpha        !\beta
%     !\gamma       !\delta        !\varepsilon   !\zeta
%     !\eta         !\theta        !\iota         !\varkappa
%     !\lambda      !\mu           !\nu           !\xi
%     !\pi          !\rho          !\sigma        !\tau
%     !\upsilon     !\phi          !\chi          !\psi
%     !\omega       !\mathit!{a!}  !\mathit!{b!}  !\chi!     % Trailing space
%     !\delta!      !\mathit!{e!}  !\phi!         !\gamma!   %
%     !\mathit!{h!} !\mathit!{i!}  !\vartheta     !\kappa!   %
%     !\lambda!     !\mathit!{m!}  !\mathit!{n!}  !\mathit!{o!}
%     !\pi!         !\theta!       !\mathit!{r!}  !\sigma!   %
%     !\tau!        !\upsilon!     !\omega!       !\xi!      %
%     !\psi!        !\mathit!{z!}  !\varphi!      !\pound\    )
%   do put(x, 'fancy!-symbol!-length, 1);

put('!\not, 'fancy!-symbol!-length, 0);

% FJW fancy!-prin2!* should do *all* (virtual) output and record
% position on the (virtual) line.  It should not do much else!

symbolic procedure fancy!-prin2!*(u,n);
   % Print (internally) u.  If n is a number then it is the width (in characters) of u.
   if numberp n then <<                 % width provided
      fancy!-pos!* := fancy!-pos!* + n;
      if fancy!-pos!* > 2*(linelength nil + 1) then overflowed!* := t; % FJW Why +1?
      fancy!-line!* := u . fancy!-line!* >>
   else                                 % look up the width
      if atom u and eqcar(explode2 u,'!\) then <<
      n := (idp u and get(u, 'fancy!-symbol!-length)) or
         (stringp u and get(intern u, 'fancy!-symbol!-length)) or 2;
      fancy!-pos!* := fancy!-pos!* + n;
      if fancy!-pos!* > 2*(linelength nil + 1) then overflowed!* := t;
      fancy!-line!* := u . fancy!-line!* >>
   else if numberp u then
      if testing!-width!* then <<
         %FJW This is a version of the block below specialised for numbers
         %FJW and intended to avoid specifying the font style.
         u := explode u;
         for each x in u do fancy!-line!* := x . fancy!-line!*;
         fancy!-pos!* := fancy!-pos!* + if numberp n then n else 2*length u;
         if fancy!-pos!* > 2*(linelength nil + 1) then overflowed!* := t;
      >>
      else fancy!-prin2number u
   else
   (begin scalar str,id; integer l;
      str := stringp u; id := idp u and not digit u; long!*:=nil;
      u:= if atom u then <<
         if !*fancy!-lower then explode2lc u
         else explode2 u >>
      else {u};
      if cdr u then long!*:=t;            %FJW identifier longer than 1 character
      if car u = '!\ then long!*:=nil;
      l := if numberp n then n else 2*length u;
      % if id and not numberp n then
      %    % Process implicit subscripts: digits within an identifier or
      %    % digits or a single letter after an underscore:
      %    u:=fancy!-lower!-digits(fancy!-esc u); % SHOULD NO LONGER BE USED!
      if long!* then
         fancy!-line!* := '!\mathit!{ . fancy!-line!*; %FJW '!\mathrm!{ . fancy!-line!*;
      for each x in u do
      <<if str and (x = blank or x = '!_)
      then fancy!-line!* := '!\ . fancy!-line!*;
         fancy!-line!* :=
            (if id and !*fancy!-lower
            then red!-char!-downcase x else x) . fancy!-line!*;
      >>;
      if long!* then fancy!-line!* := '!} . fancy!-line!*;
      fancy!-pos!* := fancy!-pos!* + l;
      if fancy!-pos!* > 2 * (linelength nil +1 ) then overflowed!*:=t;
   end) where !*lower = !*lower;

symbolic procedure fancy!-last!-symbol();
   if fancy!-line!* then car fancy!-line!*;

symbolic procedure fancy!-prin2number u;
  % we print a number eventually causing a line break
  % for very big numbers.
  if testing!-width!* then  fancy!-prin2!*(u,t) else
     fancy!-prin2number1 (if atom u then explode2 u else u);

symbolic procedure fancy!-prin2number1 u;
  begin integer c,ll;
   ll := 2 * (linelength nil +1 );
   while u do
   <<c:=c+1;
     if c>10 and fancy!-pos!* > ll then fancy!-terpri!*(t);
     fancy!-prin2!*(car u,2); u:=cdr u;
   >>;
  end;

symbolic procedure fancy!-terpri!* u;
   <<
     if fancy!-line!* then
         fancy!-page!* := fancy!-line!* . fancy!-page!*;
     fancy!-pos!* := tablevel!* * 10;
     fancy!-line!*:= {'tab . tablevel!*};
     overflowed!* := nil
   >>;

% Moved to alg/general.red so that other modules could use it when
% implementing their own custom printing.
%
%symbolic macro procedure fancy!-level u;
% % unwind-protect for special output functions.
%  {'prog,'(pos tpos fl w),
%      '(setq pos fancy!-pos!*),
%      '(setq tpos fancy!-texpos),
%      '(setq fl fancy!-line!*),
%      {'setq,'w,cadr u},
%      '(cond ((eq w 'failed)
%              (setq fancy!-line!* fl)
%              (setq fancy!-texpos tpos)
%              (setq fancy!-pos!* pos))),
%       '(return w)};

% symbolic procedure fancy!-begin();
%   % collect current status of fancy output. Return as a list
%   % for later recovery.
%   {fancy!-pos!*,fancy!-line!*};

% symbolic procedure fancy!-end(r,s);
%   % terminates a fancy print sequence. Eventually resets
%   % the output status from status record <s> if the result <r>
%   % signals an overflow.
%   <<if r='failed then
%      <<fancy!-line!*:=car s; fancy!-pos!*:=cadr s>>;
%      r>>;

symbolic procedure fancy!-mode u;
   % Get the value of the shared variable fancy_print_df or
   % fancy_lower_digits.
  begin scalar m;
     m:= lispeval u;
     if eqcar(m,'!*sq) then m:=reval m;
     return m;
  end;

%---------------- central formula converter --------------------

symbolic procedure fancy!-maprin0 u;
%%   if not overflowed!* then
   fancy!-maprint(u,0) where !*lower=nil;

symbolic procedure fancy!-maprint(l,p!*!*);
   % Print expression l at bracket level p!*!* without terminating
   % print line.  Special cases are handled by:
   %    pprifn: a print function that includes bracket level as 2nd arg.
   %     prifn: a print function with one argument.
  (begin scalar p,x,w,pos,fl;
        p := p!*!*;     % p!*!* needed for (expt a (quotient ...)) case.
        if null l then return nil;
        if atom l then return fancy!-maprint!-atom(l,p);
        pos := fancy!-pos!*; fl := fancy!-line!*;

        if not atom car l then return fancy!-maprint(car l,p);

        l := fancy!-convert(l,nil); % Convert e^x to exp(x) if x is long.

        if (x:=get(car l,'fancy!-reform)) then
          return fancy!-maprint(apply1(x,l),p);
        if ((x := get(car l,'fancy!-pprifn)) and
                   not(apply2(x,l,p) eq 'failed))
          or ((x := get(car l,'fancy!-prifn)) and
                   not(apply1(x,l) eq 'failed))
          or (get(car l,'print!-format)
                 and fancy!-print!-format(l,p) neq 'failed)
          then return nil;

        if testing!-width!* and overflowed!*
           or w='failed then return fancy!-fail(pos,fl);

        % eventually convert expression to a different form
        % for printing.

        l := fancy!-convert(l,'infix);  % Convert e^x to exp(x).

        % printing operators with integer argument in index form.
        if flagp(car l,'print!-indexed) then
        << fancy!-prefix!-operator l;
           w := fancy!-print!-indexlist cdr l
        >>

        else if x := get(car l,'infix) then
        << p := not(x>p);
          w:= if p then fancy!-in!-brackets(
            {'fancy!-inprint,mkquote car l,x,mkquote cdr l},
               '!(,'!))
              else
            fancy!-inprint(car l,x,cdr l);
        >>
        else if x:= get(car l,'fancy!-flatprifn) then
            w:=apply(x,{l})
        else
        <<
           fancy!-prefix!-operator l;
           obrkp!* := nil;
           if w neq 'failed then
             w:=fancy!-print!-function!-arguments cdr l;
        >>;

        return if testing!-width!* and overflowed!*
              or w='failed then fancy!-fail(pos,fl) else nil;
    end ) where obrkp!*=obrkp!*;

symbolic procedure fancy!-convert(l,m);
  % Convert e^x to exp(x) if appropriate.
  if eqcar(l,'expt) and cadr l= 'e and
     ( m='infix or treesizep(l,20) )
        then {'exp,caddr l}
    else l;

symbolic procedure fancy!-print!-function!-arguments u;
   % u is a function argument list.
   fancy!-in!-brackets(
      u and {'fancy!-inprint, mkquote '!*comma!*, 0, mkquote u},
      '!(,'!));

symbolic procedure fancy!-maprint!-atom(l,p);
% This should be where any atomic entity provided by the user gets
% treated. The "ordinarily special" cases are
%   (a) Things like the names "alpha", "beta", "geq", "partial-df" and
%       a whole bunch more that have a fancy!-special!-symbol property
%       indicating that they stand for some special character.
%   (b) vectors, which get displayed as eg [1,2,3,4]
%   (c) negative numbers in cases where they should be rendered in
%       parentheses to avoid ambiguity in the output.
% In the original code here all other cases where merely delegated to
% fancy!-prin2!*.
%
% There are however some "less ordinary" special cases that arise when
% material from the user clashes with TeX. I am at present aware of
% five cases of oddity:
%   (1) Strings: If the user puts a string in the input it ought to end
%                up rendered literally come what may. At present it tends
%                to get transcribed to the TeX stream unaltered, and if the
%                string has TeX special characters in it the result can be
%                odd!
%   (2) Names with special characters within. For instance "abc!%def" leads
%                to TeX that says "\mathrm{abc%def}" and the "%" there is
%                treated as a comment marker, leading to disaster.
%   (3) Names that alias a TeX directive. Eg "on revpri; (1+!\big)^3;". This
%                case can include explicit cases that could be held to
%                be deliberate such as !\alpha, but the fancy!-special!-symbol
%                scheme ought to make that unnecessary.
%   (4) Names (or strings) containing characters outside the LaTeX fonts that
%                are used by default. Mostly these will be special LaTeX
%                control characters, but e.g. if a user could get a "pounds
%                sterling" character into a name...
%   (5) All the follow-on joys that go beyond just (4) and correspond to
%       "Internationalisation"!
%
 fancy!-level
  begin scalar x;
     if (x:=get(l,'fancy!-special!-symbol)) then
         fancy!-special!-symbol(x, get(l,'fancy!-symbol!-length) or 2)
     else if vectorp l then <<
         fancy!-prin2!*("[",0);
         l:=for i:=0:upbv l collect getv(l,i);
         x:=fancy!-inprint(",",0,l);
         fancy!-prin2!*("]",0);
         return x >>
     %FJW Output strings as text rather than maths:
     %FJW The result looks OK!
     %FJW fancy!-tex!-character adds a character, escaped or replaced
     % as necessary, to fancy!-line!*.
     else if stringp l then <<
         fancy!-line!* := '!\text!{ . fancy!-line!*;
         for each c in explode2 l do fancy!-tex!-character c;
         fancy!-line!* := '!} . fancy!-line!* >>
     else if idp l then fancy!-maprint!-identifier l
     else if not numberp l or (not (l<0) or p<=get('minus,'infix))
         then fancy!-prin2!*(l,'index)
     else fancy!-in!-brackets({'fancy!-prin2!*,mkquote l,t}, '!(,'!));
     return (if testing!-width!* and overflowed!* then 'failed else nil);
  end;

symbolic procedure fancy!-maprint!-identifier ident;
   %FJW New procedure, 09/10/2020
   % ident -> ident, body123 -> body_{123}, body_123 -> body_{123},
   % body_k -> body_k, body_alpha -> body_{\alpha}.
   % Only the last _ introduces a subscript, and only if it is, or
   % translates to, a digit sequence or a single character.
   % Both body and subscript in body_subscript are processed for
   % special symbols, e.g. beta -> \beta, and TeX special characters
   % (#$%&~_\{}}) are escaped, e.g. # -> \#.
   % Special case: body_bar -> \bar{body} for a single-character body
   % or \overline{body} for a multi-character body.  This form can be
   % followed by digits or _k, which is displayed as a subscript.
   begin scalar chars, subscript, body, subscript_symbol, body_symbol, digits, bar;
      ident := explode2 ident;
      if null cdr ident then <<
         % A single-character identifier:
         ident := car ident;
         if liter ident then fancy!-prin2!*(ident, 1)
         else <<
            fancy!-prin2!*('!\mathit!{, 0);
            fancy!-tex!-character ident;
            fancy!-prin2!*('!}, 0)
         >>;
         return
      >>;

      % Search ident backwards for digits:
      chars := reverse ident;
      % Collect trailing digits, which take precedence over _:
      while chars and digit car chars do <<
         digits := car chars . digits;
         chars := cdr chars
      >>;
      if null digits then
         % Search ident backwards for _:
         while chars and not (car chars eq '!_) do <<
            subscript := car chars . subscript;
            chars := cdr chars
         >>;
      % Skip next char if it is _:
      if eqcar(chars, '!_) then chars := cdr chars;
      if subscript then
         if (bar := subscript = '(b a r)) or
         not((subscript_symbol := get(intern compress subscript, 'fancy!-special!-symbol))
            or null cdr subscript) then subscript := nil;
      % Look again for _bar:
      if (digits or subscript) and length chars > 4 and
         car chars eq 'r and cadr chars eq 'a and caddr chars eq 'b and cadddr chars eq '!_ then <<
            bar := t;
            chars := cddddr chars
         >>;
      % Retrieve identifier body:
      body := reversip chars;

      if body and (digits or subscript or bar) then <<
         % If subscript is bar then output \bar{body} after processing.
         % Otherwise, if digits then output body_{digits} after processing,
         % else subscript is, or translates to, a single character,
         % so output body_{subscript} after processing.
         body_symbol := get(intern compress body, 'fancy!-special!-symbol);
         if bar then <<
            if body_symbol or null cdr body then
               fancy!-prin2!*('!\bar!{, 0)
            else
               fancy!-prin2!*('!\overline!{, 0)
         >>;
         if body_symbol then
            fancy!-prin2!*(body_symbol, 1)
         else <<
            fancy!-prin2!*('!\mathit!{, 0);
            for each c in body do fancy!-tex!-character c;
            fancy!-prin2!*('!}, 0)
         >>;
         if bar then fancy!-prin2!*('!}, 0);
         if digits then <<
            fancy!-prin2!*('!_!{, 0);
            for each c in digits do fancy!-prin2!*(c, 1);
            fancy!-prin2!*('!}, 0)
         >> else if subscript then <<
            fancy!-prin2!*('!_, 0);
            if subscript_symbol then
               fancy!-prin2!*(subscript_symbol, 1)
            else
               fancy!-tex!-character car subscript
         >>;
         return
      >>;

      % No subscript:
      fancy!-prin2!*('!\mathit!{, 0);
      for each c in ident do fancy!-tex!-character c;
      fancy!-prin2!*('!}, 0);
   end;

fluid '(pound1!* pound2!*);

% Pounds signs are HORRID! Well all sorts of characters that are not
% in the original 96-char ASCII set are horrid, but pounds signs are
% present on an UK keyboard and that make things hurt for me! I think
% that pound1!* is WRONG now if one gets input in utf-8 and it being
% here would mess up on a unicode system. But I will still leave it for
% at least a while!
pound1!* := int2id 0x9c; % In code page 850 (ie DOS style)
pound2!* := int2id 0xa3; % Unicode

% I will force blank and tab to be declared and set here since there
% are signs that in PSL they might not be!
global '(blank tab);
blank := '! ;
tab := '!	;

symbolic procedure fancy!-tex!-character c;
   % FJW Output a single (inert) character to the (virtual) line,
   % handling special (active) TeX characters appropriately.
   <<
      fancy!-pos!* := fancy!-pos!* #+ 2;
      fancy!-line!* :=
         if c memq '(!# !$ !% !& !_ !{ !}) then c . '!\ . fancy!-line!*
         else if c eq '!~ then '!\text!{!\textasciitilde!} . fancy!-line!*
         else if c eq '!^ then '!\text!{!\textasciicircum!} . fancy!-line!*
         else if c eq '!\ then '!\text!{!\textbackslash!} . fancy!-line!*
         else if c eq blank   then '!~ . fancy!-line!*
         else if c eq tab     then <<
            fancy!-pos!* := fancy!-pos!* #+ 2;
            '!~ . '!~  . fancy!-line!* >>
         else if c eq !$eol!$ then '!\!$eol!\!$ . fancy!-line!*
         else if c eq pound1!* or c eq pound2!* then '!{!\pound!} . fancy!-line!*
         else c . fancy!-line!*;
   >>;

symbolic procedure fancy!-print!-indexlist l;
   fancy!-print!-indexlist1(l, '!_, '!,);

symbolic procedure fancy!-print!-indexlist1(l,op,sep);
  % print index or exponent lists, with or without separator.
 fancy!-level
  begin scalar w,testing!-width!*,obrkp!*;
    testing!-width!* :=t;
    fancy!-prin2!*(op,0);
    fancy!-prin2!*('!{,0);
    if null l then w:=nil
      else w:=fancy!-inprint(sep or 'times,0,l);
    fancy!-prin2!*("}",0);
    return w;
  end;

symbolic procedure fancy!-print!-one!-index i;
 fancy!-level
  begin scalar w,testing!-width!*,obrkp!*;
    testing!-width!* :=t;
    fancy!-prin2!*('!_,0);
    fancy!-prin2!*('!{,0);
    w:=fancy!-inprint('times,0,{i});
    fancy!-prin2!*("}",0);
    return w;
  end;

symbolic procedure fancy!-in!-brackets(u,l,r);
   % put form into brackets (round, curly, ...).
   % u: form to be evaluated,
   % l,r: left and right brackets to be inserted.
   fancy!-level
   (begin scalar fp,w,r1,r2,rec;
      rec := {0};
      fancy!-bstack!* := rec . fancy!-bstack!*;
      fancy!-adjust!-bkt!-levels fancy!-bstack!*;
      fp := length fancy!-page!*;
      fancy!-prin2!*(r1 := 'bkt.nil.l.rec, 2);
      % E.g. r1 = (bkt nil !( 0)
      w := eval u;
      fancy!-prin2!*(r2 := 'bkt.nil.r.rec, 2);
      % E.g. r2 = (bkt nil !) 0)
      % no line break: use \left( .. \right) pair.
      if fp = length fancy!-page!* then
         <<car cdr r1 := t; car cdr r2 := t>>;
      return w;
   end)
      where fancy!-bstack!* = fancy!-bstack!*;

symbolic procedure fancy!-adjust!-bkt!-levels u;
   if null u or null cdr u then nil
   else if caar u >= caadr u then
    <<car cadr u := car cadr u +1;
      fancy!-adjust!-bkt!-levels cdr u; >>;

symbolic procedure fancy!-exptpri(l,p);
% Prints expression in an exponent notation.
   (begin scalar !*list,pp,w,w1,w2,pos,fl;
      pos:=fancy!-pos!*; fl:=fancy!-line!*;
      w1 := cadr l; w2 := caddr l;
      pp := eqcar(w1, 'quotient) or
            eqcar(w1, 'expt) or
            (eqcar(w1, '!*hold) and not atom cadr w1);
      testing!-width!* := t;
      if eqcar(w2,'quotient) and cadr w2 = 1
          and (fixp caddr w2 or liter caddr w2) then
         return fancy!-sqrtpri!*(w1,caddr w2);
      if eqcar(w2,'quotient) and eqcar(cadr w2,'minus)
          then w2 := list('minus,list(car w2,cadadr w2,caddr w2))
          else w2 := negnumberchk w2;
      if pp then <<
          if fancy!-in!-brackets({'fancy!-maprint,
                                   mkquote w1,
                                   get('expt,'infix)},
                                 '!(, '!))='failed
            then return fancy!-fail(pos,fl) >>
      else if fancy!-maprint(w1,get('expt,'infix))='failed
            then return fancy!-fail(pos,fl);
     fancy!-prin2!*("^",0);
     if eqcar(w2,'quotient) and fixp cadr w2 and fixp caddr w2 then
      <<fancy!-prin2!*("{",0); w:=fancy!-inprint('!/,0,cdr w2);
                 fancy!-prin2!*("}",0)>>
           else w:=fancy!-maprint!-tex!-bkt(w2,0,nil);
     if w='failed then return fancy!-fail(pos,fl) ;
    end) where !*ratpri=!*ratpri,
           testing!-width!*=testing!-width!*;

put('expt,'fancy!-pprifn,'fancy!-exptpri);

symbolic procedure fancy!-inprint(op,p,l);
   % Print (internally) an infix expression.
   % op = infix operator, p = op infix precedence,
   % E.g. l = ((times a (plus x y z)) (times b (plus x y z)))
  (begin scalar x,y,w, pos,fl;
     pos:=fancy!-pos!*;
     fl:=fancy!-line!*;
      % print product of quotients using *.
     if op = 'times and eqcar(car l,'quotient) and
       cdr l and eqcar(cadr l,'quotient) then
        op:='!*;
     if op eq 'plus and !*revpri then l := reverse l;
     if not get(op,'alt) then
     <<
        if op eq 'not then
         << fancy!-oprin op;
            return  fancy!-maprint(car l,get('not,'infix));
         >>;
        if op eq 'setq and not atom (x := car reverse l)
             and idp car x and (y := getrtype x)
             and (y := get(get(y,'tag),'fancy!-setprifn))
            then return apply2(y,car l,x);
        if not atom car l and idp caar l
              and
           ((x := get(caar l,'fancy!-prifn))
                   or (x := get(caar l,'fancy!-pprifn)))
              and (get(x,op) eq 'inbrackets)
            % to avoid mix up of indices and exponents.
          then<<
               fancy!-in!-brackets(
                {'fancy!-maprint,mkquote car l,p}, '!(,'!));
              >>
           else if !*nosplit and not testing!-width!* then
                fancy!-prinfit(car l, p, nil)
           else w:=fancy!-maprint(car l, p);
          l := cdr l
      >>;
     if testing!-width!* and (overflowed!* or w='failed)
            then return fancy!-fail(pos,fl);
     if !*list and obrkp!* and memq(op,'(plus minus)) then
        % sumlevel!* is the recursion depth of fancy!-inprint applied
        % to a sum and is used only in fancy!-oprin.
        <<sumlevel!* := sumlevel!* + 1;
          tablevel!* := tablevel!* + 1>>;
     if !*nosplit and not testing!-width!* then
          % main line:
         fancy!-inprint1(op,p,l)
     else w:=fancy!-inprint2(op,p,l);
     if testing!-width!* and w='failed then return fancy!-fail(pos,fl);
   end
   ) where tablevel!*=tablevel!*, sumlevel!*=sumlevel!*;

symbolic procedure fancy!-inprint1(op,p,l);
   % main line (top level) infix printing, allow line break;
  begin scalar lop;
   for each v in l do
   <<lop := op;
     if op='plus and eqcar(v,'minus) then
       <<lop := 'minus; v:= cadr v; p:=get('minus,'infix)>>;
     if 'failed = fancy!-oprin lop then
      <<fancy!-terpri!* nil; fancy!-oprin lop>>;
     fancy!-prinfit(negnumberchk v, p, nil)
   >>;
  end;

symbolic procedure fancy!-inprint2(op,p,l);
   % second line
  begin scalar lop,w;
   for each v in l do
    if not testing!-width!* or w neq 'failed then
     <<lop:=op;
       if op='plus and eqcar(v,'minus) then
              <<lop := 'minus; v:= cadr v; p:=get('minus,'infix)>>;
       fancy!-oprin lop;
       if w neq 'failed then w:=fancy!-maprint(negnumberchk v,p)
     >>;
   return w;
  end;

symbolic procedure fancy!-inprintlist(op,p,l);
   % Print (internally) contents of an algebraic list or matrix row.
   % op is the operator, e.g. !*comma!*.
   % p is ignored
   % l is the list to print
   fancy!-level
   begin scalar fst, w, v;
  loop:
     if null l then return w;
     v := car l; l:= cdr l;
     if fst then w := fancy!-oprin op;
     if w eq 'failed  and testing!-width!* then return w;
     w := if w eq 'failed then fancy!-prinfit(v,0,op)
     else fancy!-prinfit(v,0,nil);
     if w eq 'failed and testing!-width!* then return w;
     fst := t;
     goto loop;
   end;

put('times, 'fancy!-prtch, "\*");
%FJW TeX discretionary times (\*) is not defined in LaTeX and not
%FJW supported by KaTeX, so I handle it in Run-REDUCE.

put('setq, 'fancy!-infix!-symbol, "\coloneqq "); %FJW otherwise uses prtch prop !:!=

symbolic procedure fancy!-oprin op;
 fancy!-level
  begin scalar x;
    if (x:=get(op,'fancy!-prtch)) then fancy!-prin2!*(x,1)
      else
    if (x:=get(op,'fancy!-infix!-symbol))
           then fancy!-special!-symbol(x,get(op,'fancy!-symbol!-length)
                                            or 4)
      else
    if null(x:=get(op,'prtch)) then fancy!-prin2!*(op,t)
      else
    << if !*list and obrkp!* and op memq '(plus minus)
        and (sumlevel!* = 2 or sumlevel!* = 3) % FJW hack
           % to improve `on list', but probably not right!
           % The right fix may be to change how or where sumlevel!* is
           % incremented in fancy!-inprint, but I can't see how to do it.
       then
        if testing!-width!* and not (!*acn and !*list) then return 'failed
            else fancy!-terpri!* t;
       fancy!-prin2!*(x,t);
    >>;
    if overflowed!* then return 'failed
   end;

%FJW The next two lists are based on https://katex.org/docs/supported.html#letters-and-unicode:
deflist('(
   (!Alpha "\Alpha ") (!Beta "\Beta ") (!Gamma "\Gamma ") (!Delta "\Delta ")
   (!Epsilon "\Epsilon ") (!Zeta "\Zeta ") (!Eta "\Eta ") (!Theta "\Theta ")
   (!Iota "\Iota ") (!Kappa "\Kappa ") (!Lambda "\Lambda ") (!Mu "\Mu ")
   (!Nu "\Nu ") (!Xi "\Xi ") (!Omicron "\Omicron ") (!Pi "\Pi ")
   (!Rho "\Rho ") (!Sigma "\Sigma ") (!Tau "\Tau ") (!Upsilon "\Upsilon ")
   (!Phi "\Phi ") (!Chi "\Chi ") (!Psi "\Psi ") (!Omega "\Omega ")
      ),'fancy!-special!-symbol);

deflist('(
   (!alpha "\alpha ") (!beta "\beta ") (!gamma "\gamma ") (!delta "\delta ")
   (!epsilon "\epsilon ") (!zeta "\zeta ") (!eta "\eta ") (!theta "\theta ")
   (!iota "\iota ") (!kappa "\kappa ") (!lambda "\lambda ") (!mu "\mu ")
   (!nu "\nu ") (!xi "\xi ") (!omicron "\omicron ") (!pi "\pi ")
   (!rho "\rho ") (!sigma "\sigma ") (!tau "\tau ") (!upsilon "\upsilon ")
   (!phi "\phi ") (!chi "\chi ") (!psi "\psi ") (!omega "\omega ")
      ),'fancy!-special!-symbol);

put('infinity,'fancy!-special!-symbol,"\infty ");
put('partial!-df,'fancy!-special!-symbol,"\partial ");
%put('partial!-df,'fancy!-symbol!-length,8);
put('empty!-set,'fancy!-special!-symbol,"\emptyset ");
put('not,'fancy!-special!-symbol,"\neg ");
put('not,'fancy!-infix!-symbol,"\neg ");
put('leq,'fancy!-infix!-symbol,"\leq ");
put('geq,'fancy!-infix!-symbol,"\geq ");
put('neq,'fancy!-infix!-symbol,"\neq ");
put('intersection,'fancy!-infix!-symbol,"\cap ");
put('union,'fancy!-infix!-symbol,"\cup ");
put('member,'fancy!-infix!-symbol,"\in ");
put('and,'fancy!-infix!-symbol,"\wedge ");
put('or,'fancy!-infix!-symbol,"\vee ");
put('when,'fancy!-infix!-symbol,"|");
% put('!*wcomma!*,'fancy!-infix!-symbol,",\,");
put('replaceby,'fancy!-infix!-symbol,"\Rightarrow ");
%put('replaceby,'fancy!-symbol!-length,8);
put('!~,'fancy!-functionsymbol,"\forall ");     % forall
%put('!~,'fancy!-symbol!-length,8);

% arbint, arbcomplex.
%put('arbcomplex,'fancy!-functionsymbol,227);
%put('arbint,'fancy!-functionsymbol,226);
%flag('(arbcomplex arbint),'print!-indexed);

% flag('(delta),'print!-indexed);         % Dirac delta symbol.
% David Hartley voted against..

% The following definitions allow for more natural printing of
% conditional expressions within rule lists.

symbolic procedure fancy!-condpri0 u;
   fancy!-condpri(u,0);

symbolic procedure fancy!-condpri(u,p);
 fancy!-level
  begin scalar w;
    if p>0 then fancy!-prin2 "\left(";
    while (u := cdr u) and w neq 'failed do
      <<if not(caar u eq 't)
            then <<fancy!-prin2 'if; fancy!-prin2 " ";
                   w:=fancy!-maprin0 caar u;
                   fancy!-prin2 "\,"; fancy!-prin2 'then;
                   fancy!-prin2 "\,">>;
          if w neq 'failed then w := fancy!-maprin0 cadar u;
          if cdr u then <<fancy!-prin2 "\,";
                       fancy!-prin2 'else; fancy!-prin2 "\,">>>>;
     if p>0 then fancy!-prin2 "\right)";
     if overflowed!* or w='failed then return 'failed;
   end;

put('cond,'fancy!-pprifn,'fancy!-condpri);
put('cond,'fancy!-flatprifn,'fancy!-condpri0);

symbolic procedure fancy!-revalpri u;
   fancy!-maprin0 fancy!-unquote cadr u;

symbolic procedure fancy!-unquote u;
  if eqcar(u,'list) then for each x in cdr u collect
      fancy!-unquote x
  else if eqcar(u,'quote) then cadr u else u;

put('aeval,'fancy!-prifn,'fancy!-revalpri);
put('aeval!*,'fancy!-prifn,'fancy!-revalpri);
put('reval,'fancy!-prifn,'fancy!-revalpri);
put('reval!*,'fancy!-prifn,'fancy!-revalpri);

put('aminusp!:,'fancy!-prifn,'fancy!-patpri);
put('aminusp!:,'fancy!-pat,'(lessp !&1 0));

symbolic procedure fancy!-holdpri u;
   if atom cadr u then fancy!-maprin0 cadr u
   else fancy!-in!-brackets({'fancy!-maprin0, mkquote cadr u}, '!(, '!));

put('!*hold, 'fancy!-prifn, 'fancy!-holdpri);

symbolic procedure fancy!-patpri u;
  begin scalar p;
    p:=subst(fancy!-unquote  cadr u,'!&1,
             get(car u,'fancy!-pat));
    return fancy!-maprin0 p;
  end;

symbolic procedure fancy!-boolvalpri u;
   fancy!-maprin0 cadr u;

put('boolvalue!*,'fancy!-prifn,'fancy!-boolvalpri);

symbolic procedure fancy!-quotpri u;
   begin scalar n1,n2,fl,w,pos,testing!-width!*,!*list;
     if overflowed!* or (!*acn and !*list) then return 'failed;
     testing!-width!*:=t;
     pos:=fancy!-pos!*;
     fl:=fancy!-line!*;
     fancy!-prin2!*("\frac",0);
     w:=fancy!-maprint!-tex!-bkt(cadr u,0,t);
     n1 := fancy!-pos!*;
     if w='failed
       then return fancy!-fail(pos,fl);
     fancy!-pos!* := pos;
     w := fancy!-maprint!-tex!-bkt(caddr u,0,t);
     n2 := fancy!-pos!*;
     if w='failed
       then return fancy!-fail(pos,fl);
     fancy!-pos!* := max(n1,n2);
     return t;
  end;

symbolic procedure fancy!-maprint!-tex!-bkt(u,p,m);
  % Produce expression with tex brackets {...} if
  % necessary. Ensure that {} unit is in same formula.
  % If m=t brackets will be inserted in any case.
  begin scalar w,pos,fl,testing!-width!*;
    testing!-width!*:=t;
    pos:=fancy!-pos!*;
    fl:=fancy!-line!*;
   if not m and (numberp u and 0<=u and u <=9 or liter u) then
   << fancy!-prin2!*(u,t);
      return if overflowed!* then fancy!-fail(pos,fl);
   >>;
   fancy!-prin2!*("{",0);
   w := fancy!-maprint(u,p);
   fancy!-prin2!*("}",0);
   if w='failed then return fancy!-fail(pos,fl);
  end;

symbolic procedure fancy!-fail(pos,fl);
 <<
     overflowed!* := nil;
     fancy!-pos!* := pos;
     fancy!-line!* := fl;
     'failed
 >>;

put('quotient,'fancy!-prifn,'fancy!-quotpri);

symbolic procedure fancy!-prinfit(u, p, op);
% Display u (as with maprint) with op in front of it, but starting
% a new line before it if there would be overflow otherwise.
   begin scalar pos,fl,w,ll,f;
     if pairp u and (f:=get(car u,'fancy!-prinfit)) then
        return apply(f,{u,p,op});
     pos:=fancy!-pos!*;
     fl:=fancy!-line!*;
     begin scalar testing!-width!*;
       testing!-width!*:=t;
       if op then w:=fancy!-oprin op;
       if w neq 'failed then w := fancy!-maprint(u,p);
     end;
     if w neq 'failed then return t;
     fancy!-line!*:=fl; fancy!-pos!*:=pos;
     if testing!-width!* and w eq 'failed then return w;

     if op='plus and eqcar(u,'minus) then <<op := 'minus; u:=cadr u>>;
     w:=if op then fancy!-oprin op;
       % if the operator causes the overflow, we break the line now.
     if w eq 'failed then
     <<fancy!-terpri!* nil;
       if op then fancy!-oprin op;
       return fancy!-maprint(u, p);>>;
       % if at least half the line is still free and the
       % object causing the overflow has been a number,
       % let it break.
     if fancy!-pos!* < (ll:=linelength(nil)) then
             if numberp u then return fancy!-prin2number u else
         if eqcar(u,'!:rd!:) then return fancy!-rdprin u;
       % generate a line break if we are not just behind an
       % opening bracket at the beginning of a line.
     if fancy!-pos!* > linelength nil / 2 or
          not eqcar(fancy!-last!-symbol(),'bkt) then
           fancy!-terpri!* nil;
     return fancy!-maprint(u, p);
   end;

%-----------------------------------------------------------
%
%   support for print format property
%
%-----------------------------------------------------------

symbolic procedure print_format(f,pat);
  % Assign a print pattern p to the operator form f.
put(car f, 'print!-format, (cdr f . pat) . get(car f, 'print!-format));

symbolic operator print_format;

symbolic procedure fancy!-print!-format(u,p);
 fancy!-level
  begin scalar fmt,fmtl,a;
   fmtl:=get(car u,'print!-format);
 l:
   if null fmtl then return 'failed;
   fmt := car fmtl; fmtl := cdr fmtl;
   if length(car fmt) neq length cdr u then goto l;
   a:=pair(car fmt,cdr u);
   return fancy!-print!-format1(cdr fmt,p,a);
  end;

symbolic procedure fancy!-print!-format1(u,p,a);
  begin scalar w,x,pl,bkt,obkt,q;
   if eqcar(u,'list) then u:= cdr u;
   while u and w neq 'failed do
   <<x:=car u; u:=cdr u;
     if eqcar(x,'list) then x:=cdr x;
     obkt := bkt; bkt:=nil;
     if obkt then fancy!-prin2!*('!{,0);
     w:=if pairp x then fancy!-print!-format1(x,p,a) else
        if memq(x,'(!( !) !, !. !|)) then
         <<if x eq '!( then <<pl:=p.pl; p:=0>> else
           if x eq '!) then <<p:=car pl; pl:=cdr pl>>;
           fancy!-prin2!*(x,1)>> else
        if x eq '!_ or x eq '!^ then <<bkt:=t;fancy!-prin2!*(x,0)>> else
        if q:=assoc(x,a) then fancy!-maprint(cdr q,p) else
        fancy!-maprint(x,p);
     if obkt then fancy!-prin2!*('!},0);
    >>;
    return w;
  end;

%-----------------------------------------------------------
%
%   some operator-specific print functions
%
%-----------------------------------------------------------

symbolic procedure fancy!-prefix!-operator(u);
   % FJW Display an operator identifier, possibly as a special symbol
   % that may depend on the arity.
   begin scalar sy;
      if atom u then <<
         % u is the operator identifier, for backward compatibility
         if not((sy := get(u, 'fancy!-functionsymbol))
            and (atom sy or car sy eq 'ascii)) then
               sy := get(u, 'fancy!-special!-symbol)
      >> else <<
         % u is the full sexpr (fn arg1 arg2 ...) for arity checking.
         % fancy!-functionsymbol may be a symbol or an alist of
         % (arity . symbol) pairs.
         if sy := get(car u, 'fancy!-functionsymbol) then
            if pairp sy then
               sy := (sy := assoc(length cdr u, sy)) and cdr sy;
         u := car u;
         if not sy then sy := get(u, 'fancy!-special!-symbol);
      >>;
      if sy then
         % This needs more work. Currently, fancy!-symbol!-length is
         % not used, but it could and probably should be!
         fancy!-special!-symbol(sy, get(u, 'fancy!-symbol!-length) or 2)
      else if stringp u then fancy!-prin2!*(u, t) % FJW new
      else fancy!-maprint!-identifier u; %FJW was fancy!-prin2!*(u,t);
   end;

put('sqrt,'fancy!-prifn,'fancy!-sqrtpri);

inline procedure fancy!-sqrtpri(u);
   % FJW Display the square root of u.
   fancy!-sqrtpri!*(cadr u, 2);

symbolic procedure fancy!-sqrtpri!*(u, n);
   % FJW Display the n'th root of u, where n must be a number or a
   % single character.
   fancy!-level
   begin
     if not numberp n and not liter n then return 'failed;
     fancy!-prin2!*("\sqrt", 3);        % should the width be larger?
     if n neq 2 then
     <<fancy!-prin2!*("[", 0);
       % fancy!-prin2!*("\,",1);
       fancy!-prin2!*(n, 0);     % nth root no wider than square root
       fancy!-prin2!*("]", 0);
     >>;
     return fancy!-maprint!-tex!-bkt(u, 0, t);
   end;

symbolic procedure fancy!-sub(l,p);
% Prints expression in an exponent notation.
  if get('expt,'infix)<=p then
      fancy!-in!-brackets({'fancy!-sub,mkquote l,0},'!(,'!))
    else
   fancy!-level
    begin scalar eqs,w;
      l:=cdr l;
      while cdr l do <<eqs:=append(eqs,{car l}); l:=cdr l>>;
      l:=car l;
      testing!-width!* := t;
      w := fancy!-maprint(l,get('expt,'infix));
      if w='failed then return w;
%      fancy!-prin2!*("\bigl",0);
      fancy!-prin2!*("|",1);
      fancy!-prin2!*('!_,0);
      fancy!-prin2!*("{",0);
      w:=fancy!-inprint('!*comma!*,0,eqs);
      fancy!-prin2!*("}",0);
      return w;
   end;

put('sub,'fancy!-pprifn,'fancy!-sub);

put('factorial,'fancy!-pprifn,'fancy!-factorial);

symbolic procedure fancy!-factorial(u,n);
  fancy!-level
   begin scalar w;
     w := (if atom cadr u then fancy!-maprint(cadr u,9999)
              else
           fancy!-in!-brackets({'fancy!-maprint,mkquote cadr u,0},
                               '!(,'!))
          );
     fancy!-prin2!*("!",2);
     return w;
   end;

put('binomial,'fancy!-prifn,'fancy!-binomial);

symbolic procedure fancy!-binomial u;
  fancy!-level
   begin scalar w1,w2,!*list;
     fancy!-prin2!*("\left(\begin{matrix}",2);
     w1 := fancy!-maprint(cadr u,0);
     fancy!-prin2!*("\\",0);
     w2 := fancy!-maprint(caddr u,0);
     fancy!-prin2!*("\end{matrix}\right)",2);
     if w1='failed or w2='failed then return 'failed;
   end;

symbolic procedure fancy!-intpri(u,p);
% Fancy integral print.
   if p>get('times,'infix) then
      fancy!-in!-brackets({'fancy!-intpri,mkquote u,0},'!(,'!))
   else
      fancy!-level
         begin scalar w0,w1,w2,hi,lo;
            if cdddr u then lo:=cadddr u;
            if lo and cddddr u then hi := car cddddr u;
            if fancy!-height(cadr u,1.0) > 3 then
               fancy!-prin2!*("\int ",0) % big integral wanted
            else
               fancy!-prin2!*("\int ",0);
            if lo then <<
               fancy!-prin2!*('!_,0);
               fancy!-prin2!*('!{,0);
               w0 := fancy!-maprint(lo,0) where !*list=nil;
               fancy!-prin2!*('!},0);
            >>;
            if hi then <<
               fancy!-prin2!*('!^,0);
               fancy!-maprint!-tex!-bkt(hi,0,t) where !*list=nil;
            >>;
            w1:=fancy!-maprint(cadr u,0);
            fancy!-prin2!*("\,d\,",2);
            w2:=fancy!-maprint(caddr u,0);
            if w1='failed or w2='failed or w0='failed then return 'failed;
         end;

symbolic procedure fancy!-height(u,h);
  % Fancy height. Estimate the height of an expression, this is a
  % subroutine of fancy!-intpri.
    if atom u then h
    else if car u = 'minus then fancy!-height(cadr u,h)
    else if car u = 'plus or car u = 'times then
      eval('max. for each w in cdr u collect fancy!-height(w,h))
    else if car u = 'expt then
         fancy!-height(cadr u,h) + fancy!-height(caddr u,h*0.8)
    else if car u = 'quotient then
         fancy!-height(cadr u,h) + fancy!-height(caddr u,h)
    else if get(car u,'simpfn) then fancy!-height(cadr u,h)
    else h;

put('int,'fancy!-pprifn,'fancy!-intpri);

symbolic procedure fancy!-sumpri!*(u,p,mode);
  if p>get('minus,'infix) then
    fancy!-in!-brackets({'fancy!-sumpri!*,mkquote u,0,mkquote mode},
                         '!(,'!))
   else
  fancy!-level
   begin scalar w,w0,w1,lo,hi,var;
     var := caddr u;
     if cdddr u then lo:=cadddr u;
     if lo and cddddr u then hi := car cddddr u;
     w:=if lo then {'equal,var,lo} else var;
     if mode = 'sum then
        fancy!-prin2!*("\sum",0) % big SIGMA
     else if mode = 'prod then
        fancy!-prin2!*("\prod",0); % big PI
     fancy!-prin2!*('!_,0);
     fancy!-prin2!*('!{,0);
     if w then w0:=fancy!-maprint(w,0) where !*list=nil;
     fancy!-prin2!*('!},0);
     if hi then <<fancy!-prin2!*('!^,0);
                  fancy!-maprint!-tex!-bkt(hi,0,nil) where !*list=nil;
                 >>;
     fancy!-prin2!*('!\!, ,1);
     w1:=fancy!-maprint(cadr u,0);
     if w0='failed or w1='failed then return 'failed;
   end;

symbolic procedure fancy!-sumpri(u,p); fancy!-sumpri!*(u,p,'sum);

put('sum,'fancy!-pprifn,'fancy!-sumpri);
put('infsum,'fancy!-pprifn,'fancy!-sumpri);

symbolic procedure fancy!-prodpri(u,p); fancy!-sumpri!*(u,p,'prod);

put('prod,'fancy!-pprifn,'fancy!-prodpri);

symbolic procedure fancy!-limpri(u,p);
  if p>get('minus,'infix) then
    fancy!-in!-brackets({'fancy!-sumpri,mkquote u,0},'!(,'!))
   else
  fancy!-level
   begin scalar w,lo,var;
     var := caddr u;
     if cdddr u then lo:=cadddr u;
     fancy!-prin2!*("\lim",6);
     fancy!-prin2!*('!_,0);
     fancy!-prin2!*('!{,0);
     fancy!-maprint(var,0);
     fancy!-prin2!*("\rightarrow ",0);
     fancy!-prin2!*('! ,0); % make sure there is space before the following symbol
     fancy!-maprint(lo,0) where !*list=nil;
     fancy!-prin2!*('!},0);
     w:=fancy!-maprint(cadr u,0);
     return w;
   end;

put('limit,'fancy!-pprifn,'fancy!-limpri);

symbolic procedure fancy!-listpri(u);
   % Print (internally) an algebraic list.
   % u = (list ...)
   fancy!-level
      if null cdr u then fancy!-maprint('empty!-set, 0)
      else
         fancy!-in!-brackets(
            {'fancy!-inprintlist, mkquote '!*comma!*, 0, mkquote cdr u},
            '!{,'!});

put('list,'fancy!-prifn,'fancy!-listpri);
put('list,'fancy!-flatprifn,'fancy!-listpri);

put('!*sq,'fancy!-reform,'fancy!-sqreform);

symbolic procedure fancy!-sqreform u;
   << u := cadr u;
      if !*pri or wtl!* then prepreform prepsq!* sqhorner!* u
       else if denr u = 1 then fancy!-sfreform numr u
       else {'quotient,fancy!-sfreform numr u,fancy!-sfreform denr u} >>;

symbolic procedure fancy!-sfreform u;
    begin scalar z;
      while not domainp u do <<z := fancy!-termreform lt u . z; u := red u >>;
      if not null u then z := prepd u . z;
      return replus reversip z;
   end;

symbolic procedure fancy!-termreform u;
     begin scalar v,w,z,sgn;
	v := tc u;
      	u := tpow u;
      	if (w := kernlp v) and not !:onep w
        then <<v := quotf(v,w);
               if minusf w then <<sgn := t; w := !:minus w>>>>;
      if w and not !:onep w
        then z := (if domainp w then prepd w else w) . z;
      z := fancy!-powerreform u . z;
      if not(domainp v and !:onep v) then z := fancy!-sfreform v . z;
      z := retimes reversip z;
      if sgn then z := {'minus,z};
      return z;
     end;

symbolic procedure fancy!-powerreform u;
   begin scalar b;
      % Process main variable.
      if atom car u then b := car u
       else if not atom caar u then b := fancy!-sfreform car u
       else if caar u eq '!*sq then b := fancy!-sqreform cadar u
       else b := car u;
      % Process degree.
      if (u := pdeg u)=1 then return b
      else return {'expt,b,u}
   end;

put('df,'fancy!-pprifn,'fancy!-dfpri);

global '(!*dfprint);

symbolic procedure fancy!-dfpri(u,l);
   % E.g. u = (df f x y) or (df (g x y) x y)
   if !*dfprint then
      fancy!-dfpriindexed(
         if atom cadr u then u else car u . caadr u . cddr u, l)
   else
   (if flagp(cadr u,'print!-indexed) or
      pairp cadr u and flagp(caadr u,'print!-indexed)
   then fancy!-dfpriindexed(u,l)
   else if m = 'partial then fancy!-dfpri0(u,l,'partial!-df)
   else if m = 'total then fancy!-dfpri0(u,l,'!d)
   else if m = 'indexed then fancy!-dfpriindexed(u,l)
   else rederr "unknown print mode for DF")
      where m=fancy!-mode('fancy_print_df);

symbolic procedure fancy!-partialdfpri(u,l);
     fancy!-dfpri0(u,l,'partial!-df);

symbolic procedure fancy!-dfpri0(u,l,symb);
 if null cddr u then fancy!-maprin0{'times,symb,cadr u} else
 if l >= get('expt,'infix) then % brackets if exponented
  fancy!-in!-brackets({'fancy!-dfpri0,mkquote u,0,mkquote symb},
                      '!(,'!))
   else
 fancy!-level
  begin scalar x,d,q; integer n,m;
    u:=cdr u;
    q:=car u;
    u:=cdr u;
    while u do
    <<x:=car u; u:=cdr u;
      if u and numberp car u then
      <<m:=car u; u := cdr u>> else m:=1;
      n:=n+m;
      d:= append(d,{symb,if m=1 then x else {'expt,x,m}});
    >>;
    return fancy!-maprin0
    {'quotient, {'times,if n=1 then symb else
                                    {'expt,symb,n},q},
       'times. d};
  end;

symbolic procedure fancy!-dfpriindexed(u,l);
   if null cddr u then fancy!-maprin0{'times,'partial!-df,cadr u} else
   begin scalar w;
      w:=fancy!-maprin0 cadr u;
      if testing!-width!* and w='failed then return w;
      w :=fancy!-print!-indexlist fancy!-dfpriindexedx(cddr u,nil);
      return w;
   end;

symbolic procedure fancy!-dfpriindexedx(u,p);
  if null u then nil else
  if numberp car u then
   append(for i:=2:car u collect p,fancy!-dfpriindexedx(cdr u,p))
     else
  car u . fancy!-dfpriindexedx(cdr u,car u);

put('!:rd!:,'fancy!-prifn,'fancy!-rdprin);
put('!:rd!:,'fancy!-flatprifn,'fancy!-rdprin);

symbolic procedure fancy!-rdprin u;
 fancy!-level
  begin scalar digits; integer dotpos,xp;
   u:=rd!:explode u;
   digits := car u; xp := cadr u; dotpos := caddr u;
   return fancy!-rdprin1(digits,xp,dotpos);
  end;

symbolic procedure fancy!-rdprin1(digits,xp,dotpos);
  begin scalar str;
   if xp>0 and dotpos+xp<length digits-1 then
      <<dotpos := dotpos+xp; xp:=0>>;
    % build character string from number.
   for i:=1:dotpos do
   <<str := car digits . str;
     digits := cdr digits; if null digits then digits:='(!0);
   >>;
   str := '!. . str;
   for each c in digits do str :=c.str;
   if not(xp=0) then
   <<str:='!e.str;
     for each c in explode2 xp do str:=c.str>>;
   if testing!-width!* and
      fancy!-pos!* + 2*length str > 2 * linelength nil then
        return 'failed;
   fancy!-prin2number1 reversip str;
  end;

put('!:cr!:,'fancy!-pprifn,'fancy!-cmpxprin);
put('!:cr!:,'fancy!-pprifn,'fancy!-cmpxprin);

symbolic procedure fancy!-cmpxprin(u,l);
   begin scalar rp,ip;
     rp:=reval {'repart,u}; ip:=reval {'impart,u};
     return fancy!-maprint(
       if ip=0 then rp else
       if rp=0 then {'times,ip,'!i} else
        {'plus,rp,{'times,ip,'!i}},l);
   end;

symbolic procedure fancy!-dn!:prin u;
 begin scalar lst; integer dotpos,ex;
  lst := bfexplode0x (cadr u, cddr u);
  ex := cadr lst;
  dotpos := caddr lst;
  lst := car lst;
  return fancy!-rdprin1 (lst,ex,dotpos)
 end;

put ('!:dn!:, 'fancy!-prifn, 'fancy!-dn!:prin);

% on fancy; %FJW fmp!-switch t;

endmodule;


%-------------------------------------------------------

module f;   % Matrix printing routines.


fluid '(!*nat);

fluid '(obrkp!*);

symbolic procedure fancy!-setmatpri(u,v);
   fancy!-matpri1(cdr v,u);

put('mat,'fancy!-setprifn,'fancy!-setmatpri);

symbolic procedure fancy!-matpri u;
   fancy!-matpri1(cdr u,nil);


put('mat,'fancy!-prifn,'fancy!-matpri);

symbolic procedure fancy!-matpri1(u,x);
   % Prints a matrix canonical form U with name X.
   % Tries to do fancy display if nat flag is on.
  begin scalar w;
     w := fancy!-matpri2(u,x,nil);
     if w neq 'failed or testing!-width!* then return w;
     fancy!-matpri3(u,x);
  end;

symbolic procedure fancy!-matpri2(u,x,bkt);
  % Tries to print matrix as compact block.
  fancy!-level
    begin scalar w,testing!-width!*,fl,fp,fmat,row,elt,fail;
      integer cols,rows,rw,maxpos;
      testing!-width!*:=t;
      rows := length u;
      cols := length car u;
      if cols*rows>400 then return 'failed;

      if x then
      << fancy!-maprint(x,0); fancy!-prin2!*(":=",4) >>;
      fl := fancy!-line!*; fp := fancy!-pos!*;
         %  remaining room for the columns.
      rw := linelength(nil)-2 -(fancy!-pos!*+2);
      rw := rw/cols;
      fmat := for each row in u collect
        for each elt in row collect
          if not fail then
          <<fancy!-line!*:=nil; fancy!-pos!*:=0;
            w:=fancy!-maprint(elt,0);
            if fancy!-pos!*>maxpos then maxpos:=fancy!-pos!*;
            if w='failed or fancy!-pos!*>rw
              then fail:=t else
               (fancy!-line!*.fancy!-pos!*)
          >>;
     if fail then return 'failed;
     testing!-width!* := nil;
       % restore output line.
     fancy!-pos!* := fp; fancy!-line!* := fl;
       % TEX header
     fancy!-prin2!*(bldmsg("\left%w\begin{matrix}",
                        if bkt then car bkt else "("),0);
       % join elements.
     while fmat do
     <<row := car fmat; fmat:=cdr fmat;
       while row do
       <<elt:=car row; row:=cdr row;
         fancy!-line!* := "\displaystyle " . fancy!-line!*; %FJW
         fancy!-line!* := append(car elt,fancy!-line!*);
         if row then fancy!-line!* :='!& . fancy!-line!*
          else if fmat then
             fancy!-line!* := "\\[1.5em]" . fancy!-line!*; %FJW
       >>;
     >>;
     fancy!-prin2!*(bldmsg("\end{matrix}\right%w",
                        if bkt then cdr bkt else ")"),0);
      % compute total horizontal extent of matrix
     fancy!-pos!* := fp + maxpos*(cols+1);
    return t;
    end;


symbolic procedure fancy!-matpri3(u,x);
  if null x then fancy!-matpriflat('mat.u) else
   begin scalar obrkp!*,!*list;
      integer r,c;
      obrkp!* := nil;
      if null x then x:='mat;
      % fancy!-terpri!*;                  % missing arg!!!
      for each row in u do
      <<r:=r+1; c:=0;
        for each elt in row do
        << c:=c+1;
           if not !*nero then
           << fancy!-prin2!*(x,t);
              fancy!-print!-indexlist {r,c};
              fancy!-prin2!*(":=",t);
              fancy!-maprint(elt,0);
              fancy!-terpri!* t;
           >>;
        >>;
      >>;
   end;

symbolic procedure fancy!-matpriflat(u);
 begin
  fancy!-oprin 'mat;
  fancy!-in!-brackets(
   {'fancy!-matpriflat1,mkquote '!*comma!*,0,mkquote cdr u},
               '!(,'!));
 end;

symbolic procedure fancy!-matpriflat1(op,p,l);
   % Print (internally) the rows of a matrix.
   begin scalar fst, w;
      for each v in l do <<
         if fst then <<
            fancy!-oprin op;
            % If the next row does not fit on the current print line
            % then move it completely to a new line:
            w := fancy!-level
               fancy!-in!-brackets(
                  {'fancy!-inprintlist, mkquote '!*comma!*, 0, mkquote v},
                  '!(,'!)) where testing!-width!* = t;
         >>;
         if w eq 'failed then fancy!-terpri!* t;
         if not fst or w eq 'failed then
            fancy!-in!-brackets(
               {'fancy!-inprintlist, mkquote '!*comma!*, 0, mkquote v},
               '!(,'!));
         fst := t;
      >>;
   end;

put('mat,'fancy!-flatprifn,'fancy!-matpriflat);

symbolic procedure fancy!-matfit(u,p,op);
% Prinfit routine for matrix.
% a new line before it if there would be overflow otherwise.
 fancy!-level
   begin scalar pos,fl,fp,w,ll;
     pos:=fancy!-pos!*;
     fl:=fancy!-line!*;
     begin scalar testing!-width!*;
       testing!-width!*:=t;
       if op then w:=fancy!-oprin op;
       if w neq 'failed then w := fancy!-matpri(u);
     end;
     if w neq 'failed or
       (w eq 'failed and testing!-width!*) then return w;
     fancy!-line!*:=fl; fancy!-pos!*:=pos; w:=nil;
     fp := fancy!-page!*;
% matrix: give us a second chance with a fresh line
     begin scalar testing!-width!*;
       testing!-width!*:=t;
       if op then w:=fancy!-oprin op;
       fancy!-terpri!* nil;
       if w neq 'failed then w := fancy!-matpri u;
     end;
     if w neq 'failed then return t;
     fancy!-line!*:=fl; fancy!-pos!*:=pos; fancy!-page!*:=fp;

     ll:=linelength nil;
     if op then fancy!-oprin op;
     if atom u or fancy!-pos!* > ll / 2 then fancy!-terpri!* nil;
     return fancy!-matpriflat(u);
   end;

put('mat,'fancy!-prinfit,'fancy!-matfit);

put('taylor!*,'fancy!-reform,'taylor!*print1);

endmodule;


module fancy_standard_functions;

% Display transcendental functions following the NIST Digital Library
% of Mathematical Functions, http://dlmf.nist.gov/.

% Elementary transcendental functions

put('sin, 'fancy!-functionsymbol, "\sin");
put('cos, 'fancy!-functionsymbol, "\cos");
put('tan, 'fancy!-functionsymbol, "\tan");
put('cot, 'fancy!-functionsymbol, "\cot");
put('sec, 'fancy!-functionsymbol, "\sec");
put('csc, 'fancy!-functionsymbol, "\csc");

put('sinh, 'fancy!-functionsymbol, "\sinh");
put('cosh, 'fancy!-functionsymbol, "\cosh");
put('tanh, 'fancy!-functionsymbol, "\tanh");
put('coth, 'fancy!-functionsymbol, "\coth");
put('sech, 'fancy!-functionsymbol, "\mathrm{sech}");
put('csch, 'fancy!-functionsymbol, "\mathrm{csch}");

% The inverse of the trigonometric or hyperbolic function fn is named
% arcfn and is written in normal (roman) font style.

put('asin, 'fancy!-functionsymbol, "\arcsin");
put('acos, 'fancy!-functionsymbol, "\arccos");
put('atan, 'fancy!-functionsymbol, "\arctan");
put('acot, 'fancy!-functionsymbol, "\mathrm{arccot}");
put('asec, 'fancy!-functionsymbol, "\mathrm{arcsec}");
put('acsc, 'fancy!-functionsymbol, "\mathrm{arccsc}");

put('asinh, 'fancy!-functionsymbol, "\mathrm{arcsinh}");
put('acosh, 'fancy!-functionsymbol, "\mathrm{arccosh}");
put('atanh, 'fancy!-functionsymbol, "\mathrm{arctanh}");
put('acoth, 'fancy!-functionsymbol, "\mathrm{arccoth}");
put('asech, 'fancy!-functionsymbol, "\mathrm{arcsech}");
put('acsch, 'fancy!-functionsymbol, "\mathrm{arccsch}");

put('exp, 'fancy!-functionsymbol, "\exp"); % Used in special cases, e.g. complicated argument.
put('log, 'fancy!-functionsymbol, "\log");
put('logb, 'fancy!-prifn, 'fancy!-logb);
put('log10, 'fancy!-prifn, 'fancy!-log10);

symbolic procedure fancy!-logb(u);
   % u = (logb(x, b) -> \log_{b}(x)
   fancy!-indexed!-fn {'log, caddr u, cadr u};

symbolic procedure fancy!-log10(u);
   % u = (log10 x) -> \log_{10}(x)
   fancy!-indexed!-fn {'log, 10, cadr u};

symbolic inline procedure fancy!-indexed!-fn u;
   fancy!-bessel u;

put('ln, 'fancy!-functionsymbol, "\ln");
put('max, 'fancy!-functionsymbol, "\max");
put('min, 'fancy!-functionsymbol, "\min");
put('repart, 'fancy!-functionsymbol, "\Re");
put('impart, 'fancy!-functionsymbol, "\Im");
put('repart, 'fancy!-symbol!-length, 4); % wide symbols
put('impart, 'fancy!-symbol!-length, 4);

for each x in '(
   sin     cos     tan     cot     sec     csc
   sinh    cosh    tanh    coth    sech    csch
   exp     log     ln      max     min
   ) do put(x, 'fancy!-symbol!-length, 2*length explode2 x);

for each x in '(
   arcsin  arccos  arctan  arccot  arcsec  arccsc
   arcsinh arccosh arctanh arccoth arcsech arccsch
   ) do put(x, 'fancy!-symbol!-length, 2*(length explode2 x + 2));

put('abs, 'fancy!-prifn, 'fancy!-abs);

symbolic procedure fancy!-abs u;
   fancy!-level
   begin scalar w;
      fancy!-prin2!*("\left|", 1);
      w := fancy!-maprin0 cadr u;
      fancy!-prin2!*("\right|", 1);
      return w
   end;

% Gamma, Beta and Related Functions

put('Euler_gamma, 'fancy!-special!-symbol, "\gamma ");
put('Gamma, 'fancy!-functionsymbol, '((1 . "\Gamma "))); % unary only
put('polygamma, 'fancy!-prifn, 'fancy!-polygamma);

symbolic procedure fancy!-polygamma(u);
   % u = (polygamma n z) -> \psi^{(n)}(z)
   fancy!-level
   begin scalar w;
      fancy!-prefix!-operator "\psi";
      fancy!-prin2!*('!^, 0);  fancy!-prin2!*('!{, 0);
      w := fancy!-in!-brackets({'fancy!-maprin0, mkquote cadr u}, '!(, '!));
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!}, 0);
      return fancy!-in!-brackets({'fancy!-maprin0, mkquote caddr u}, '!(, '!));
   end;

put('iGamma, 'fancy!-functionsymbol, '!P); % P(a,z)

put('iBeta, 'fancy!-prifn, 'fancy!-iBeta);
put('iBeta, 'fancy!-functionsymbol, '!I);

symbolic procedure fancy!-iBeta(u);
   % u = (iBeta a b x) -> I_{x}(a,b)
   fancy!-indexed!-fn({car u, cadddr u, cadr u, caddr u});

put('dilog, 'fancy!-functionsymbol, "\mathrm{Li}_2"); % roman Li_2(z)
put('dilog, 'fancy!-symbol!-length, 5);

put('Pochhammer, 'fancy!-prifn, 'fancy!-Pochhammer); % (a)_{n}

symbolic procedure fancy!-Pochhammer(u);
   % u = (Pochhammer a n) -> (a)_{n}
   fancy!-level
   begin scalar w;
      w := fancy!-in!-brackets({'fancy!-maprin0, mkquote cadr u}, '!(, '!));
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!_, 0);  fancy!-prin2!*('!{, 0);
      fancy!-maprin0 caddr u;
      fancy!-prin2!*('!}, 0);
   end;

% Integral Functions

put('Ei, 'fancy!-functionsymbol, "\mathrm{Ei}");
put('Si, 'fancy!-functionsymbol, "\mathrm{Si}");
put('Ci, 'fancy!-functionsymbol, "\mathrm{Ci}");
put('Shi, 'fancy!-functionsymbol, "\mathrm{Shi}");
put('Chi, 'fancy!-functionsymbol, "\mathrm{Chi}");
put('erf, 'fancy!-functionsymbol, "\mathrm{erf}");
put('Fresnel_S, 'fancy!-functionsymbol, "\mathrm{S}");
put('Fresnel_C, 'fancy!-functionsymbol, "\mathrm{C}");

for each x in '(Ei Si Ci Shi Chi erf) do
   put(x, 'fancy!-symbol!-length, 2*length explode2 x);

% Airy, Bessel and Related Functions

put('Airy_Ai, 'fancy!-functionsymbol, "\mathrm{Ai}");
put('Airy_Bi, 'fancy!-functionsymbol, "\mathrm{Bi}");
put('Airy_Ai, 'fancy!-symbol!-length, 4);
put('Airy_Bi, 'fancy!-symbol!-length, 4);
put('Airy_AiPrime, 'fancy!-functionsymbol, "\mathrm{Ai}'");
put('Airy_BiPrime, 'fancy!-functionsymbol, "\mathrm{Bi}'");
put('Airy_AiPrime, 'fancy!-symbol!-length, 5);
put('Airy_BiPrime, 'fancy!-symbol!-length, 5);

put('BesselI, 'fancy!-prifn, 'fancy!-bessel);
put('BesselJ, 'fancy!-prifn, 'fancy!-bessel);
put('BesselY, 'fancy!-prifn, 'fancy!-bessel);
put('BesselK, 'fancy!-prifn, 'fancy!-bessel);
put('BesselI, 'fancy!-functionsymbol, '!I);
put('BesselJ, 'fancy!-functionsymbol, '!J);
put('BesselY, 'fancy!-functionsymbol, '!Y);
put('BesselK, 'fancy!-functionsymbol, '!K);

symbolic procedure fancy!-bessel(u);
 fancy!-level
  begin scalar w;
   fancy!-prefix!-operator car u;
   w:=fancy!-print!-one!-index cadr u;
   if testing!-width!* and w eq 'failed then return w;
   return fancy!-print!-function!-arguments cddr u;
  end;

put('Hankel1, 'fancy!-prifn, 'fancy!-Hankel); % H_{nu}^{(1)}(z)
put('Hankel2, 'fancy!-prifn, 'fancy!-Hankel); % H_{nu}^{(2)}(z)

symbolic procedure fancy!-Hankel(u);
   % u = (Hankel1/2 nu z)
   fancy!-level
   begin scalar w;
      fancy!-prefix!-operator '!H;
      w:=fancy!-print!-one!-index cadr u;
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!^, 0);
      fancy!-prin2!*('!{, 0);  fancy!-prin2!*('!(, 0);
      fancy!-prin2!*(if car u eq 'Hankel1 then 1 else 2, 0);
      fancy!-prin2!*('!), 0);  fancy!-prin2!*('!}, 0);
      return fancy!-print!-function!-arguments cddr u;
   end;

% Struve, Lommel, Kummer, Whittaker and Spherical Harmonic Functions

put('StruveH, 'fancy!-prifn, 'fancy!-indexed!-fn); % bold H_{nu}(z)
put('StruveH, 'fancy!-functionsymbol, "\mathbf{H}");
put('StruveL, 'fancy!-prifn, 'fancy!-indexed!-fn); % bold L_{nu}(z)
put('StruveL, 'fancy!-functionsymbol, "\mathbf{L}");

put('Lommel1, 'fancy!-prifn, 'fancy!-Lommel); % s_{mu,nu}(z)
put('Lommel2, 'fancy!-prifn, 'fancy!-Lommel); % S_{mu,nu}(z)

symbolic procedure fancy!-Lommel(u);
   % u = (Lommel1/2 mu nu z)
   fancy!-level
   begin scalar w;
      fancy!-prefix!-operator(if car u eq 'Lommel1 then '!s else '!S);
      w := fancy!-print!-indexlist1({cadr u, caddr u}, '!_, '!*comma!*);
      if testing!-width!* and w eq 'failed then return w;
      return fancy!-print!-function!-arguments cdddr u;
   end;

put('KummerM, 'fancy!-functionsymbol, '!M); % M(a, b, z)
put('KummerU, 'fancy!-functionsymbol, '!U); % U(a, b, z)

% Classical Orthogonal Polynomials

put('JacobiP, 'fancy!-prifn,'fancy!-JacobiP); % P_n^{(alpha, beta)}(x)

symbolic procedure fancy!-JacobiP(u);
   % u = (JacobiP n alpha beta x)
   fancy!-level
   begin scalar w;
      fancy!-prefix!-operator '!P;
      w := fancy!-print!-one!-index cadr u;
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!^, 0);
      fancy!-prin2!*('!{, 0);
      w := fancy!-print!-function!-arguments {caddr u, cadddr u};
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!}, 0);
      return fancy!-print!-function!-arguments cddddr u;
   end;

put('GegenbauerP, 'fancy!-prifn, 'fancy!-Gegenbauer!-style); % C_n^{(lamda)}(x)
put('GegenbauerP, 'fancy!-functionsymbol, '!C);

symbolic procedure fancy!-Gegenbauer!-style(u);
   % u = (GegenbauerP n lamda x)
   fancy!-level
   begin scalar w;
      fancy!-prefix!-operator car u;
      w := fancy!-print!-one!-index cadr u;
      if testing!-width!* and w eq 'failed then return w;
      fancy!-prin2!*('!^, 0);
      fancy!-prin2!*('!{, 0);  fancy!-prin2!*('!(, 0);
      fancy!-maprint(caddr u, 0);
      fancy!-prin2!*('!), 0);  fancy!-prin2!*('!}, 0);
      return fancy!-print!-function!-arguments cdddr u;
   end;

put('ChebyshevT, 'fancy!-prifn, 'fancy!-indexed!-fn); % T_n(x)
put('ChebyshevT, 'fancy!-functionsymbol, '!T);
put('ChebyshevU, 'fancy!-prifn, 'fancy!-indexed!-fn); % U_n(x)
put('ChebyshevU, 'fancy!-functionsymbol, '!U);

put('LegendreP, 'fancy!-prifn, 'fancy!-Legendre!-style);
put('LegendreP, 'fancy!-functionsymbol, '!P);

symbolic procedure fancy!-Legendre!-style(u);
   % u = (LegendreP n x) -> P_n(x)
   % u = (LegendreP n m x) -> P_n^{(m)}(x)
   if length u = 3 then fancy!-indexed!-fn(u)
   else fancy!-Gegenbauer!-style(u);

put('LaguerreP, 'fancy!-prifn, 'fancy!-Legendre!-style);
put('LaguerreP, 'fancy!-functionsymbol, '!L);

put('HermiteP, 'fancy!-prifn, 'fancy!-indexed!-fn); % H_n(x)
put('HermiteP, 'fancy!-functionsymbol, '!H);

% Other Special Functions

put('polylog, 'fancy!-prifn, 'fancy!-indexed!-fn);
put('polylog, 'fancy!-functionsymbol, "Li");
put('polylog, 'fancy!-symbolic!-length, 4);

% Hypergeometric Functions

put('hypergeometric,'fancy!-prifn,'fancy!-hypergeometric);

symbolic procedure fancy!-hypergeometric u;
 fancy!-level
  begin scalar w,a1,a2,a3;
   a1 :=cdr cadr u;
   a2 := cdr caddr u;
   a3 := cadddr u;
   fancy!-prin2!*("{}", 0);
   w:=fancy!-print!-one!-index length a1;
   if testing!-width!* and w eq 'failed then return w;
   fancy!-prin2!*("F", 2);
   w:=fancy!-print!-one!-index length a2;
   if testing!-width!* and w eq 'failed then return w;
   fancy!-prin2!*("\left(\left.", 1);
   fancy!-prin2!*("{}", 0);
   if null a1 then a1 := list '!-;
   if null a2 then a2 := list '!-;
   w := w eq 'failed or fancy!-print!-indexlist1(a1, '!^, '!*comma!*);
   w := w eq 'failed or fancy!-print!-indexlist1(a2, '!_, '!*comma!*);
   fancy!-prin2!*("\,", 1);
   %w := w eq 'failed or fancy!-special!-symbol(124,1);    % vertical bar
   fancy!-prin2!*("\right|\,", 2);
   w := w eq 'failed or fancy!-prinfit(a3, 0, nil);
   fancy!-prin2!*("\right)", 1);
   return w;
  end;

% hypergeometric({1,2,u/w,v},{5,6},sqrt x);

put('MeijerG,'fancy!-prifn,'fancy!-meijerg);

symbolic procedure fancy!-meijerg u;
 fancy!-level
  begin scalar w,a1,a2,a3;
   integer n,m,p,q;
   a1 :=cdr cadr u;
   a2 := cdr caddr u;
   a3 := cadddr u;
   m:=length cdar a2;
   n:=length cdar a1;
   a1 := append(cdar a1 , cdr a1);
   a2 := append(cdar a2 , cdr a2);
   p:=length a1; q:=length a2;
   fancy!-prin2!*("G", 2);
   w := w eq 'failed or
        fancy!-print!-indexlist1({m,n},'!^,nil);
   w := w eq 'failed or
        fancy!-print!-indexlist1({p,q},'!_,nil);
   fancy!-prin2!*("\left(", 1);
   w := w eq 'failed or fancy!-prinfit(a3,0,nil);
   fancy!-prin2!*("\,", 1);
   %w := w eq 'failed or fancy!-special!-symbol(124,1);    % vertical bar
   fancy!-prin2!*("\left|\,{}", 2);
   if null a1 then a1 := list '!-;
   if null a2 then a2 := list '!-;
   w := w eq 'failed or fancy!-print!-indexlist1(a1, '!^, '!*comma!*);
   w := w eq 'failed or fancy!-print!-indexlist1(a2, '!_, '!*comma!*);
   fancy!-prin2!*("\right.\right)", 1);
   return w;
  end;

% meijerg({{},1},{{0}},x);

%ACN Now a few things that can be useful for testing this code...

symbolic <<
% Arrange that if this file is loaded twice you do not get silly messages
% to do with redefinition of these.
  if not get('texsym, 'simpfn) then
    algebraic operator texsym, texbox, texfbox, texstring >>;

% texsym(!Longleftarrow) should generate \Longleftarrow (etc). This
% might plausibly be useful while checking that the interface can render
% all TeX built-in keywords properly. Furthermore I allow extra args, so
% that eg texsym(stackrel,f,texsym(longrightarrow)) turns into
%   \stackrel{f}{\longrightarrow}

put('texsym,'fancy!-prifn,'fancy!-texsym);

symbolic procedure fancy!-texsym u;
   fancy!-level
    begin
      if null u then return;
      fancy!-prin2 list2string ('!\ . explode2 cadr u);
      u := cddr u;
      while u do <<
         fancy!-line!* := "{" . fancy!-line!*;
         fancy!-maprint(car u, 0);
         fancy!-line!* := "}" . fancy!-line!*;
         u := cdr u >>
    end;

% texstring("arbitrary tex stuff",...)
% where atoms (eg strings and words) are just passed to tex but
% more complicated items go through fancy!-maprint.

put('texstring,'fancy!-prifn,'fancy!-texstring);

symbolic procedure fancy!-texstring u;
   fancy!-level
    for each s in cdr u do <<
      if not atom s then fancy!-maprint(s, 0)
      else <<
         if not stringp s then s := list2string explode2 s;
         fancy!-line!* := s . fancy!-line!* >> >>;

% texbox(h) is a box of given height (in points)
% texbox(h, d) is a box of given height and depth
%              height is amount above the reference line, depth is amount
%              below.
% textbox(h, d, c) is a box of given size with some specified content

% All these draw a frame around the space used so you can see what is
% goin on.

% The idea that this may be useful when checking how layouts cope with
% various sizes of content, eg big delimiters, square root signs etc. So I
% can test with "for i := 10:40 do write sqrt(texbox(i))" etc.
% to test sqrt with arguments of height 10, 11, ... to 40 points. Note that
% certainly with the CSL version the concept of a "point" is a bit vauge!
% However if I were to imagine that my screen was at 75 pixels per inch I
% could with SOME reason interpret point as meaning pixel, and that is
% what I will do. At present what I might do about hard-copy output is
% pretty uncertain. If height and depth are given as 0 and there is a
% content them the content will define the box size.

put('texbox,'fancy!-prifn,'fancy!-texbox);

symbolic procedure fancy!-texbox u;
   fancy!-level
    begin
      scalar height, depth, contents;
      contents := nil;
      u := cdr u;
      height := car u;
      u := cdr u;
      if u then <<
         depth := car u;
         u := cdr u;
         if u then contents := car u >>;
      if not numberp height then height:=0;
      if not numberp depth then depth:=0;
      if height=0 and depth=0 and null contents then height:=10;
      fancy!-prin2 "\fbox{";
      if height neq 0 or depth neq 0 then << % insert a rule
         fancy!-line!* := "\rule" . fancy!-line!*;
         if depth neq 0 then <<
            fancy!-line!* := "[-" . fancy!-line!*;
            fancy!-line!* := depth . fancy!-line!*;
            fancy!-line!* := "pt]" . fancy!-line!* >>;
         fancy!-line!* := "{0pt}{" . fancy!-line!*;
         fancy!-line!* := (height+depth) . fancy!-line!*;
         fancy!-line!* := "pt}" . fancy!-line!* >>;
      if contents then contents := fancy!-maprint(contents, 0)
      else fancy!-line!* := "\rule{10pt}{0pt}" . fancy!-line!*;
      fancy!-prin2 "}";
      return contents
    end;

% texfbox is a simplified version of texbox, and just draws a box around the
% expression it is given.

put('texfbox,'fancy!-prifn,'fancy!-texfbox);

symbolic procedure fancy!-texfbox u;
   fancy!-level
    begin
      fancy!-prin2 "\fbox{";
      u := fancy!-maprint(cadr u, 0);
      fancy!-prin2 "}";
      return u
    end;

endmodule;


module rrprint_redfront;

% Code based on the redfront package to support font colouring for
% non-typeset algebraic-mode output.  To enable set
% outputhandler!* := 'coloured!-output;

fluid '(orig!*);

procedure coloured!-output(mode,l);
   begin scalar outputhandler!*;
      if mode eq 'maprin then
         if ofl!* or posn!* neq orig!* then
            maprin l
	 else <<
            coloured!-output!-on();
	    assgnpri(l,nil,nil);
            coloured!-output!-off()
         >>
      else if mode eq 'prin2!* then
         prin2!* l
      else if mode eq 'terpri then
         terpri!* l
      else if mode eq 'assgnpri then <<
         coloured!-output!-on();
         % All args needed for matrix assignments:
         assgnpri(car l, cadr l, caddr l);
         coloured!-output!-off()
      >> else
         rederr {"unknown method ", mode, " in coloured!-output"}
   end;

procedure coloured!-output!-on();
   <<
      terpri!* nil;
      prin2 int2id 3;
      terpri!* nil
   >>;

procedure coloured!-output!-off();
   <<
      terpri!* nil;
      prin2 int2id 4
   >>;

procedure coloured!-output!-formwrite(u,vars,mode);
   % Workaround to avoid linebreaks between elements output by write.
   begin scalar z;
      z := formwrite(u,vars,mode);
      if z then return {'cond,
         {{'and,{'eq,'outputhandler!*,'(quote coloured!-output)},'(not ofl!*)},
            {'prog,'(outputhandler!*),'(coloured!-output!-on),z,'(coloured!-output!-off)}},
         {t,z}}
   end;

put('write, 'formfn, 'coloured!-output!-formwrite);

endmodule;

end;
