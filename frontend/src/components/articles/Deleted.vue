<template>
  <div style="display: flex; justify-content: center">
    <div>
      <el-input
        v-model="title"
        style="width: 200px"
        placeholder="请输入标题"
        clearable
      />
      <el-button>
        <el-icon style="font-size: 20px" @click="getTitle()"
          ><Search
        /></el-icon>
      </el-button>
    </div>
  </div>

  <div style="margin-top: 2%">
    <!-- 这里可以通过height设置滚动栏的高度。需要设置，否则就会无限延伸，左边导航出错-->
    <el-scrollbar style="height: 100%">
      <!-- 在不同的el-card之间加上空白，vertical表示垂直方向-->
      <el-space direction="vertical">
        <el-card
          v-for="data in articleList"
          :key="data.id"
          style="width: 1000px"
          shadow="hover"
        >
          <div @click="router.push('detail/' + data.id)">
            <div style="font-weight: bold">{{ data.title }}</div>
            <div style="margin-top: 1%">具体内容点击查看</div>
          </div>
          <div
            style="
              display: flex;
              margin-top: 1%;
              justify-content: space-between;
            "
          >
            <div style="display: flex">
              <el-badge :value="data.likeNum">
                <el-button>{{ data.isLiked ? "取消点赞" : "点赞" }}</el-button>
              </el-badge>
              <el-badge :value="data.collectNum" class="item">
                <el-button>{{
                  data.isCollected ? "取消收藏" : "收藏"
                }}</el-button>
              </el-badge>
              <el-badge :value="data.commentNum" class="item">
                <el-button>评论</el-button>
              </el-badge>
            </div>
            <div style="align-content: center">
              更新时间：{{ data.updateTime }}
            </div>
            <div>
              <el-button type="success" @click="handleRecover(data.id)"
                >恢复</el-button
              >
              <el-button type="info" @click="handleDelete(data.id)"
                >删除</el-button
              >
            </div>
          </div>
        </el-card>
      </el-space>
    </el-scrollbar>
  </div>
</template>

<script setup>
import router from "@/router";
import { useUserStore } from "@/stores/user";
import { storeToRefs } from "pinia";
import {
  getDeleted,
  getByTitle,
  deleteArticle,
  recover,
} from "@/api/article.js";
import { ElMessage } from "element-plus";
import { onMounted } from "vue";

let { userId } = storeToRefs(useUserStore());
// 模糊查询用到的标题
let title = ref("");
// 查询到的博客列表
let articleList = ref({});

// 页面一加载就查询出对应的数据用于展示
onMounted(() => {
  getArticleDeleted();
});

function getArticleDeleted() {
  getDeleted(userId.value).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      articleList.value = res.data;
    }
  });
}

function getTitle() {
  getByTitle(userId.value, title.value, 2).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      articleList.value = res.data;
      ElMessage.success("查询成功");
    }
  });
}

function handleDelete(id) {
  ElMessageBox.confirm("确认删除该博客吗？该操作不可逆", "删除博客", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      deleteArticle(id).then((response) => {
        let res = response.data;
        if (res.code == 200) {
          ElMessage.success("删除成功");
          // 删除之后更新页面
          getArticleDeleted();
        }
      });
    })
    .catch(() => {});
}

function handleRecover(id) {
  recover(id).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      // 恢复成功之后也应该刷新页面
      ElMessage.success("恢复成功");
      getArticleDeleted();
    }
  });
}
</script>

<style scoped>
.item {
  display: flex;
  margin-left: 10%;
}
</style>
