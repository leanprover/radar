import { defineConfig } from "eslint/config";
import js from "@eslint/js";
import ts from "typescript-eslint";
import vue from "eslint-plugin-vue";
import prettier from "eslint-config-prettier";

export default defineConfig([
  { ignores: ["dist/"] },

  js.configs.recommended,
  ts.configs.strict,
  ts.configs.stylistic,
  vue.configs["flat/recommended"],

  // Parse vue files correctly
  { files: ["**/*.vue"], languageOptions: { parserOptions: { parser: ts.parser } } },

  // Exception for shadcn/vue components
  { ignores: ["src/components/ui/**"] },

  // Exception for typed paths
  { files: ["src/pages/**/*.vue"], rules: { "vue/multi-word-component-names": "off" } },

  prettier,
]);
