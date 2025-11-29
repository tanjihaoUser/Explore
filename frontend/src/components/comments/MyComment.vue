<template>
  <div>
    <el-scrollbar style="height: 100%">
      <el-space direction="vertical">
        <el-card
          v-for="data in commentList"
          :key="data.id"
          style="width: 1000px; display: flex; justify-content: space-between"
          shadow="hover"
          @click="router.push('/index/article/detail/' + data.articleId)"
        >
          <div>
            <div style="color: orange">
              {{ data.username }}
            </div>
            <div style="margin-top: 15%">{{ data.articleTitle }}</div>
          </div>
          <div style="display: flex; align-items: center">
            {{ data.updateTime }}
          </div>
        </el-card>
      </el-space>
    </el-scrollbar>
  </div>
</template>

<script setup>
import router from "@/router";
import { useUserStore } from "@/stores/user";
import { getUserComments } from "@/api/comment";
import { storeToRefs } from "pinia";
import { onMounted } from "vue";

let { userId } = storeToRefs(useUserStore());
// 查询到的博客列表
let commentList = ref({});

// 页面一加载就查询出对应的数据用于展示
onMounted(() => {
  getAllComments();
});

function getAllComments() {
  getUserComments(userId.value).then((response) => {
    let res = response.data;
    console.log(res.data);
    if (res.code == 200) {
      commentList.value = res.data;
    }
  });
}
</script>

<style scoped>
.item {
  display: flex;
  margin-left: 10%;
}
.el-card ::v-deep .el-card__body {
  background-color: lightblue;
  display: flex;
  justify-content: space-between;
  width: 1000px;
}
</style>
