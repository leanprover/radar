import { createApp } from "vue";
import "./style.css";
import App from "./App.vue";
import { VueQueryPlugin } from "@tanstack/vue-query";

createApp(App).use(VueQueryPlugin, { enableDevtoolsV6Plugin: true }).mount("#app");
