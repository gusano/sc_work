#### Color-theme-bluish

To use this color-theme, you need the `emacs-goodies-el` package installed.
Copy `color-theme-bluish.el` in your `~/.emacs.d` folder and add the following
lines to your `~/.emacs` config file:

    (require 'color-theme)
    (color-theme-initialize)
    (load-file "~/.emacs.d/color-theme-bluish.el")
    (color-theme-bluish)
    
    ;-- colorize numbers 
    (global-font-lock-mode 1)
    (font-lock-add-keywords 'sclang-mode
        '(("\\b[-]?[0-9]+[e]?[-]?[0-9]?[.]?[0-9]*\\b" . font-lock-warning-face)))
    
    ;-- colorize parenthesis, brackets, ...
    (font-lock-add-keywords 'sclang-mode
        '(("[\(\)\{\}\|]" . font-lock-builtin-face)))
