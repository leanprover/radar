import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";
import { createRouter, createWebHistory } from "vue-router";
import { routes } from "vue-router/auto-routes";
import { createPinia } from "pinia";
import { VueQueryPlugin } from "@tanstack/vue-query";

// eslint-disable-next-line @typescript-eslint/no-unsafe-argument
createApp(App)
  .use(createRouter({ history: createWebHistory(), routes }))
  .use(createPinia())
  .use(VueQueryPlugin, { enableDevtoolsV6Plugin: true })
  .mount("#app");
