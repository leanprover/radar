import { defineConfig } from "vite";
import vueRouter from "unplugin-vue-router/vite";
import vue from "@vitejs/plugin-vue";
import vueDevTools from "vite-plugin-vue-devtools";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vueRouter(), vue(), vueDevTools(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    proxy: {
      "/api": process.env["VITE_PROXY"] ?? "http://localhost:8080/",
    },
  },
});
