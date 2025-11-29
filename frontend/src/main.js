import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";

import App from "./App.vue";
import router from "./router";

const app = createApp(App);

const pinia = createPinia(); //创建pinia实例
pinia.use(piniaPluginPersistedstate); //将插件添加到 pinia 实例上

app.use(pinia);
app.use(router);

// 引入Markdown编辑器
import VMdEditor from "@kangc/v-md-editor";
import "@kangc/v-md-editor/lib/style/base-editor.css";
import githubTheme from "@kangc/v-md-editor/lib/theme/github.js";
import "@kangc/v-md-editor/lib/theme/style/github.css";
// 引入Markdown预览器，用于渲染Markdown
import VMdPreview from "@kangc/v-md-editor/lib/preview";
import hljs from "highlight.js";

VMdEditor.use(githubTheme, {
  Hljs: hljs,
});

VMdPreview.use(githubTheme, {
  Hljs: hljs,
});
app.use(VMdEditor);
app.use(VMdPreview);

// element-plus
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";

app.use(ElementPlus);
// 注册所有图标
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.mount("#app");
