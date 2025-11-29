// 引入pinia
import { defineStore } from "pinia";
import { login, validateSession } from "@/api/session.js";

// 定义 store 并导出
export const useUserStore = defineStore("user", {
  // 定义状态数据
  state: () => ({
    userId: undefined,
    username: undefined,
    sessionId: undefined,
  }),

  // 获取数据的方法
  getters: {
    isLoggedIn: (state) => {
      return state.userId !== undefined && state.sessionId !== undefined;
    },
  },

  // 修改数据方法
  actions: {
    setUserInfo(id, name, sessionId) {
      this.userId = id;
      this.username = name;
      this.sessionId = sessionId;
    },
    
    clearUserInfo() {
      this.userId = undefined;
      this.username = undefined;
      this.sessionId = undefined;
    },
    
    async checkLoginStatus() {
      if (!this.sessionId) {
        return false;
      }
      try {
        const response = await validateSession(this.sessionId);
        const res = response.data || {};

        // 兼容两种响应格式：
        // 1) { code: 200, data: { valid: true }, ... }
        // 2) { success: true, data: { valid: true }, ... }
        const hasCode = typeof res.code !== "undefined" && res.code !== null;
        const isSuccessByCode = hasCode && res.code === 200;
        const isSuccessByFlag = !hasCode && res.success === true;

        const isValid = (isSuccessByCode || isSuccessByFlag) && res.data && res.data.valid === true;

        if (!isValid) {
          // 会话无效时清理本地信息
          this.clearUserInfo();
        }

        return isValid;
      } catch (error) {
        // 调用失败视为未登录，不抛出异常以免打断导航
        this.clearUserInfo();
        return false;
      }
    },
    
    updateCurrentPage(path) {
      if (this.sessionId) {
        // 可以在这里调用 API 更新当前页面
        // updateCurrentPage(this.sessionId, path);
      }
    },
    
    recordActivity() {
      if (this.sessionId) {
        // 可以在这里调用 API 记录活动
        // recordActivity(this.sessionId);
      }
    },
  },

  // 使用持久化
  persist: {
    enabled: true,
    storage: localStorage,
    key: "userInfo",
    paths: ["userId", "username", "sessionId"],
  },
});
