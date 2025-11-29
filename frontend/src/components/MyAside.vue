<template>
  <!-- router表示是否启用vue-router模式，启用该模式会在激活导航时以index作为path进行路由跳转-->
  <el-menu
    active-text-color="#ffd04b"
    background-color="#545c64"
    class="el-menu-vertical-demo"
    :default-active="$route.path"
    text-color="#fff"
    :collapse="!expand"
    :style="menuStyle"
    :router="true"
  >
    <el-menu-item index="/home">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </el-menu-item>

    <el-menu-item index="/timeline">
      <el-icon><Clock /></el-icon>
      <span>时间线</span>
    </el-menu-item>

    <el-menu-item index="/ranking">
      <el-icon><Trophy /></el-icon>
      <span>排行榜</span>
    </el-menu-item>

    <el-menu-item index="/create-post">
      <el-icon><Edit /></el-icon>
      <span>发布新帖</span>
    </el-menu-item>

    <el-sub-menu index="profile">
      <template #title>
        <el-icon><User /></el-icon>
        <span>个人中心</span>
      </template>
      <el-menu-item index="/profile">我的主页</el-menu-item>
      <el-menu-item index="/favorites">我的收藏</el-menu-item>
      <el-menu-item index="/following">我的关注</el-menu-item>
      <el-menu-item index="/followers">我的粉丝</el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>

<script setup>
import { useMenuStore } from "@/stores/menu.js";
import { storeToRefs } from "pinia";
import { computed } from "vue";

const menuStore = useMenuStore();
let { expand } = storeToRefs(menuStore);

// expand=true 表示展开（显示文字），expand=false 表示收缩（只显示图标）
// 展开时保持原来的宽度，收起时缩小到只显示图标
const menuStyle = computed(() => ({
  height: "100vh",
  width: expand.value ? "200px" : "64px",
  transition: "width 0.2s ease",
}));
</script>

<style lang="scss" scoped></style>
