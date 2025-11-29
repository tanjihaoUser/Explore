// 引入pinia
import { defineStore } from "pinia";
// 定义 store 并导出
export const useMenuStore = defineStore("menu", {
  // 定义状态数据
  state: () => ({
    expand: false,
  }),

  // 获取数据的方法
  getters: {
    Boolean: (state) => state.expand,
  },

  // 修改数据方法
  actions: {
    setExpand(a) {
      this.expand = a;
    },
  },

  // 使用持久化
  persist: {
    enabled: true,
    storage: localStorage,
    key: "menuInfo",
    path: ["expand"],
  },
});
