/*
*  basic polyfill for CSS.escape()
*  https://drafts.csswg.org/cssom/#the-css.escape()-method
*/
;(function (window) {

  var root = typeof window != 'undefined' ? window : this

if (root.CSS && root.CSS.escape) {
  return root.CSS.escape;
}

var cssEscape = function (value) {
  if (arguments.length == 0) {
    throw new TypeError('`CSS.escape` requires an argument.');
  }
  var permittedChars = /[0-9a-zA-Z_-]/,
      string = String(value),
          length = string.length,
      index = -1,
      result = '';

  while (++index < length) {
          var char = string.charAt(index)
    result += char.match(permittedChars) ? char : '\\' + char;
  }
  return result;
};

if (!root.CSS) {
  root.CSS = {};
}

root.CSS.escape = cssEscape;
return cssEscape;

})(window);