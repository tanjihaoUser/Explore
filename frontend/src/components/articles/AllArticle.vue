<template>
  <div style="display: flex; justify-content: space-between">
    <div>
      <el-input
        v-model="queryForm.title"
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

    <div>
      <el-input
        v-model="queryForm.username"
        style="width: 200px"
        placeholder="请输入用户名"
        clearable
      />
      <el-button>
        <el-icon style="font-size: 20px" @click="getByName"><Search /></el-icon>
      </el-button>
    </div>
    <div style="margin-right: 10%">
      <el-button type="primary" round @click="router.push('handle/0')"
        >新增博客</el-button
      >
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
          @click.native="getDetail(data.id)"
        >
          <div style="font-weight: bold">{{ data.title }}</div>
          <div style="margin-top: 1%">具体内容点击查看</div>
          <div style="display: flex; margin-top: 1%">
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
            <div style="margin-left: 20%; align-content: center">
              更新时间：{{ data.updateTime }}
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
import { getAll, getByTitle, getByUsername } from "@/api/article.js";
import { ElMessage } from "element-plus";
import { onMounted } from "vue";

let { userId } = storeToRefs(useUserStore());
let queryForm = ref({
  title: "",
  username: "",
});

let articleList = ref({});

onMounted(() => {
  getAllArticle();
});

function getAllArticle() {
  getAll(userId.value).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      articleList.value = res.data;
    }
  });
}

function getTitle() {
  getByTitle(userId.value, queryForm.value.title, 0).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      articleList.value = res.data;
      ElMessage.success("查询成功");
    }
  });
}

function getByName() {
  getByUsername(userId.value, queryForm.value.username).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      articleList.value = res.data;
      ElMessage.success("查询成功");
    }
  });
}

function getDetail(articleId) {
  router.push("detail/" + articleId);
}
</script>

<style scoped>
.item {
  display: flex;
  margin-left: 2%;
}
</style>
