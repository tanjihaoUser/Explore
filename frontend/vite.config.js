import { fileURLToPath, URL } from "node:url";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import Icons from "unplugin-icons/vite";
import IconsResolver from "unplugin-icons/resolver";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ["vue", "vue-router"],
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver(), IconsResolver()],
    }),
    Icons(),
  ],
  resolve: {
    // 使用import导入文件时刻省略后缀
    extensions: [".js", ".vue", ".json", ".css"],
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
