<template>
  <!-- 需要鉴权的页面使用带有头部和侧边栏的主布局 -->
  <el-container v-if="useMainLayout">
    <el-aside width="auto" collapse-transition="false">
      <MyAside />
    </el-aside>
    <el-container>
      <el-header style="height: 10vh">
        <MyHeader />
      </el-header>
      <el-main style="height: 90vh">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>

  <!-- 登录、注册等无需布局的页面，直接渲染路由视图 -->
  <RouterView v-else />
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import MyAside from "@/components/MyAside";
import MyHeader from "@/components/MyHeader";

const route = useRoute();

// 仅在需要鉴权的页面（meta.requiresAuth === true）使用主布局
const useMainLayout = computed(() => {
  return route.meta.requiresAuth === true;
});
</script>

<style scoped>
</style>
