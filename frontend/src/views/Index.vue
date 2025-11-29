<template>
  <el-container>
    <el-aside width="auto" collapse-transition="false"><MyAside /></el-aside>
    <el-container>
      <el-header style="height: 10vh"><MyHeader /> </el-header>
      <el-main style="height: 90vh">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import MyAside from "@/components/MyAside";
import MyHeader from "@/components/MyHeader";
</script>

<style lang="scss" scoped></style>
