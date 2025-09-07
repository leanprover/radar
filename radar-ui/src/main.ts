import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";
import { VueQueryPlugin } from "@tanstack/vue-query";
import { createRouter, createWebHistory } from "vue-router";
import { routes } from "vue-router/auto-routes";

const router = createRouter({
  history: createWebHistory(),
  routes,
});

createApp(App).use(router).use(VueQueryPlugin, { enableDevtoolsV6Plugin: true }).mount("#app");
