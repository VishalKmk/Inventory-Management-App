{
  "root": true,
  "extends": [
    "next/core-web-vitals",
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:prettier/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "plugins": [
    "@typescript-eslint",
    "prettier"
  ],
  "rules": {
    "prettier/prettier": [
      "error",
      {
        "endOfLine": "auto",
        "singleQuote": true,
        "semi": true,
        "tabWidth": 2,
        "printWidth": 100,
        "trailingComma": "es5"
      }
    ],
    "@typescript-eslint/no-unused-vars": [
      "warn",
      {
        "argsIgnorePattern": "^_",
        "varsIgnorePattern": "^_",
        "caughtErrorsIgnorePattern": "^_"
      }
    ],
    "@typescript-eslint/no-explicit-any": "warn",
    "no-console": [
      "warn",
      {
        "allow": [
          "warn",
          "error",
          "info"
        ]
      }
    ]
  },
  "ignorePatterns": [
    "node_modules/",
    ".next/",
    "out/",
    "public/"
  ],
  "env": {
    "browser": true,
    "node": true,
    "es6": true
  }
}