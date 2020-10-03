# REDUCE Typeset Maths #

## Francis Wright, October 2020 ##

This is an attempt to document the `tmprint` package, which is used
internally by the CSL REDUCE GUI and the REDUCE TeXmacs interface to
generate TeX markup for typeset maths display.  Closely related code
is used for the same purpose by Run-REDUCE.

The (shared) variable `fancy_print_df` can be set to one of the values
`partial`, `total` or `indexed` to control the display of derivatives.
The default value is `partial`.  The derivative `df(f,x,2,y)`
(assuming `depend f,x,y`) is displayed as

* partial: $\frac{\partial^3 f}{\partial x^2 \partial y}$;
* total: $\frac{d^3 f}{d x^2 d y}$;
* indexed: $f_{xxy}$.

An operator declared `print_indexed` has its arguments displayed as
indices, e.g. after `print_indexed a;` the operator value `a(i,2)` is
displayed as $a{i2}$.
