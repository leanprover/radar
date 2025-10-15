import js from "@eslint/js";
import prettier from "eslint-config-prettier";
import vue from "eslint-plugin-vue";
import { defineConfig } from "eslint/config";
import globals from "globals";
import ts from "typescript-eslint";

export default defineConfig([
  { ignores: ["dist/"] },

  js.configs.recommended,
  ts.configs.strictTypeChecked,
  ts.configs.stylisticTypeChecked,
  vue.configs["flat/recommended"],

  // Type information for better ts linting
  {
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
        extraFileExtensions: ["vue"],
      },
    },
  },

  // We're in the browser
  { files: ["src/**"], languageOptions: { globals: { ...globals.browser } } },

  // Parse vue files correctly
  { files: ["**/*.vue"], languageOptions: { parserOptions: { parser: ts.parser } } },

  // Exception for typed paths
  { files: ["src/pages/**/*.vue"], rules: { "vue/multi-word-component-names": "off" } },

  // Custom lints
  {
    rules: {
      // https://eslint.org/docs/latest/rules/
      eqeqeq: "error",

      // https://eslint.vuejs.org/rules/
      "vue/block-lang": ["error", { script: { lang: "ts" } }],
      "vue/block-order": ["error", { order: ["script", "template", "style"] }],
      "vue/eqeqeq": "error",
      "vue/v-for-delimiter-style": ["error", "in"], // "in" has proper syntax highlighting
    },
  },

  prettier,
]);
