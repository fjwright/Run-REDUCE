# REDUCE Typeset Maths #

## Francis Wright, October 2020 ##

This is an attempt to document the options that are readily available
to users for controlling the `tmprint` package, which is used
internally by the CSL REDUCE GUI and the REDUCE TeXmacs interface to
generate TeX markup for typeset maths display.  Closely related code
is used for the same purpose by Run-REDUCE.

### Derivatives ###

The (shared) variable `fancy_print_df` can be set to one of the values
`partial`, `total` or `indexed` to control the display of derivatives.
The default value is `partial`.  The derivative `df(f,x,2,y)`
(assuming `depend f,x,y`) is displayed as

* `partial`: $\frac{\partial^3 f}{\partial x^2 \partial y}$;
* `total`: $\frac{d^3 f}{d x^2 d y}$;
* `indexed`: $f_{xxy}$.

### Operators ###

An operator declared `print_indexed` has its arguments displayed as
indices, e.g. after `print_indexed a;` the operator value `a(i,2)` is
displayed as $a{i2}$.

You can also declare several operators together to be indexed, e.g.
`print_indexed(y, z);` and operators can be n-ary, e.g. `y(2,3)` and
`z(i,j)` display like $y_{2 3}$ and $z_{i j}$.

### Digits in Identifiers ###

The (shared) variable `fancy_lower_digits` can be set to one of the
values `t`, `nil` or `all` to control the display of digits within
identifiers.  The default value is `t`.  Digits in an identifier are
typeset as subscripts if `fancy_lower_digits = all` or if
`fancy_lower_digits = t` and the digits are all at the end.  With the
following values assigned to `fancy_lower_digits`

* `t`: `ab12cd34` is displayed as $ab12cd34$, `abcd34` is displayed as
  $abcd_{34}$;
* `all`: `ab12cd34` is displayed as $ab_{12}cd_{34}$, `abcd34` is
  displayed as $abcd_{34}$;
* `nil`: `ab12cd34` is displayed as $ab12cd34$, `abcd34` is displayed
  as $abcd34$.
