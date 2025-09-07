import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";
import { VueQueryPlugin } from "@tanstack/vue-query";
import { createRouter, createWebHistory } from "vue-router";
import CRepoOverview from "@/components/CRepoOverview.vue";
import CRepoNoneSelected from "@/components/CRepoNoneSelected.vue";
import CQueue from "@/components/CQueue.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "root", component: CRepoNoneSelected },
    { path: "/repo/:repo/", name: "overview", component: CRepoOverview },
    { path: "/queue/", name: "queue", component: CQueue },
  ],
});

createApp(App).use(router).use(VueQueryPlugin, { enableDevtoolsV6Plugin: true }).mount("#app");
