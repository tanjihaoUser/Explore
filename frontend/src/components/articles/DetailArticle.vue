<template>
  <el-scrollbar>
    <!-- 文章标题-->
    <h4 style="display: flex; justify-content: center">
      {{ detailArticle.title }}
    </h4>

    <!-- 分隔线-->
    <el-divider />

    <!-- 主要内容-->
    <v-md-preview :text="detailArticle.content"></v-md-preview>

    <el-divider />

    <!-- 收藏、点赞及更新时间-->
    <div style="display: flex; margin-top: 1%">
      <el-badge :value="detailArticle.likeNum">
        <el-button @click="handleArticleLike()">{{
          detailArticle.isLiked ? "取消点赞" : "点赞"
        }}</el-button>
      </el-badge>
      <el-badge :value="detailArticle.collectNum" class="item">
        <el-button @click="handleCollect()">{{
          detailArticle.isCollected ? "取消收藏" : "收藏"
        }}</el-button>
      </el-badge>
      <el-badge :value="detailArticle.commentNum" class="item">
        <el-button>评论</el-button>
      </el-badge>
      <div style="margin-left: 20%; align-content: center">
        更新时间：{{ detailArticle.updateTime }}
      </div>
    </div>

    <el-divider />

    <!-- 评论部分-->
    <div style="display: flex; justify-content: center">
      <el-input
        v-model="comment.content"
        clearable
        :placeholder="
          comment.index == -1 ? '请输入评论' : '回复' + comment.index + '楼'
        "
        style="width: 50%"
        size="large"
      ></el-input>
      <el-button
        type="primary"
        @click="publishComment()"
        style="margin-left: 5%"
        >发表</el-button
      >
      <el-button type="info" @click="comment.index = -1">取消</el-button>
    </div>

    <!-- 具体评论-->
    <div
      style="display: flex; justify-content: center; margin-top: 2%"
      id="comment"
    >
      <el-space direction="vertical">
        <el-card
          v-for="(data, index) in detailArticle.comments"
          :key="data.commentId"
          style="width: 1000px"
          shadow="hover"
          :id="data.commentId"
        >
          <div
            @click="
              comment.index = index + 1;
              comment.parent = data.commentId;
            "
          >
            <div
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: 18px;
              "
            >
              <div style="color: orange">
                {{ data.username }}
              </div>
              <div>{{ index + 1 }}楼</div>
            </div>
            <div
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
              "
            >
              <div style="margin-left: 10%">
                {{ data.content }}
              </div>
              <div style="margin-right: 20%">
                <el-badge :value="data.likeNum">
                  <el-button @click="handleCommentLike(data)">{{
                    data.isLiked ? "取消点赞" : "点赞"
                  }}</el-button>
                </el-badge>
              </div>
            </div>
            <el-card
              v-show="data.parent != -1"
              style="
                background-color: aliceblue;
                margin-top: 1%;
                width: 80%;
                margin-left: 10%;
              "
            >
              Re：{{ data.parentComment }}
            </el-card>
          </div>
          <div
            style="
              margin-top: 2%;
              display: flex;
              justify-content: space-between;
              align-content: center;
              align-items: center;
            "
          >
            <div>{{ data.updateTime }}</div>
            <!-- 自己发表的评论才展示编辑和删除-->
            <div style="margin-right: 5%; display: flex">
              <div v-show="data.userId == userId">
                <el-button
                  type="success"
                  @click="handleChange(data.commentId, data.content)"
                  >编辑</el-button
                >
                <el-button type="primary" @click="handleDelete(data.commentId)"
                  >删除</el-button
                >
              </div>
            </div>
          </div>
        </el-card>
      </el-space>
    </div>
  </el-scrollbar>

  <el-dialog v-model="changeVisible">
    <el-input v-model="changeData.content" clearable></el-input>
    <div style="justify-content: center; display: flex; margin-top: 2%">
      <el-button type="primary" @click="editComment()">确认</el-button>
      <el-button type="info" @click="changeVisible = false">取消</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { useUserStore } from "@/stores/user";
import {
  getArticleDetail,
  addArticleLike,
  subArticleLike,
  addCollect,
  subCollect,
} from "@/api/article";
import {
  addComment,
  getByArticleId,
  changeCommentDetail,
  addCommentLike,
  subCommentLike,
  deleteComment,
} from "@/api/comment";
import { storeToRefs } from "pinia";
import { onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";

// 获取博客ID和用户ID
let articleId = useRoute().params.articleId;
let userStore = useUserStore();
let { userId } = storeToRefs(userStore);
let changeVisible = ref(false);
// 修改评论时传给后端的数据，评论ID和修改后评论（后端定义为ID，这里不使用commentID）
let changeData = ref({
  id: undefined,
  content: undefined,
});

// 具体的博客信息
let detailArticle = ref({});

// 具体的评论，用户ID和博客ID都一致，直接写死在这里
let comment = ref({
  userId: userId.value,
  articleId: articleId,
  content: "",
  parent: -1,
  index: -1,
});

// （取消）博客点赞收藏以及评论点赞发送给后端数据
let relation = ref({
  userId: userId.value,
  variousId: undefined,
  type: undefined,
  newCount: undefined,
});

// 页面一加载就查询出对应博客并展示
onMounted(() => {
  getArticleDetail(userId.value, articleId).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      detailArticle.value = res.data;
    }
  });
});

// 处理博客的点赞和取消点赞。0表示博客点赞。现根据已有信息判断是否点赞
// 先向后端发起请求，请求成功再修改前端数据
// 传输数据时只传递增值，可能点击时未刷新页面，点赞和收藏数已经发生变化，使用原有值出错
function handleArticleLike() {
  relation.value.type = 0;
  relation.value.variousId = detailArticle.value.id;
  if (detailArticle.value.isLiked) {
    relation.value.newCount = -1;
    subArticleLike(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  } else {
    relation.value.newCount = 1;
    addArticleLike(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  }
  detailArticle.value.isLiked = !detailArticle.value.isLiked;
  detailArticle.value.likeNum += relation.value.newCount;
}

function handleCollect() {
  relation.value.type = 1;
  relation.value.variousId = detailArticle.value.id;
  if (detailArticle.value.isCollected) {
    relation.value.newCount = -1;
    subCollect(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  } else {
    relation.value.newCount = 1;
    addCollect(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  }
  detailArticle.value.isCollected = !detailArticle.value.isCollected;
  detailArticle.value.collectNum += relation.value.newCount;
}

function handleCommentLike(data) {
  relation.value.type = 2;
  relation.value.variousId = data.commentId;
  if (data.isLiked) {
    relation.value.newCount = -1;
    subCommentLike(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  } else {
    relation.value.newCount = 1;
    addCommentLike(relation.value).then((response) => {
      let res = response.data;
      if (res.code != 200) return;
    });
  }
  data.isLiked = !data.isLiked;
  data.likeNum += relation.value.newCount;
}

// 发表、修改和删除评论后都需要更新评论区（重新查询评论）
function publishComment() {
  addComment(comment.value).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      ElMessage.success("发表评论成功");
      getByArticleId(userId.value, articleId).then((response) => {
        let res = response.data;
        if (res.code == 200) {
          detailArticle.value.comments = res.data;
        }
      });
      // 发表评论之后评论内容置空。同时前端评论数显式加一
      // 前面调用的后端接口已经修改数据库中评论数
      comment.value.content = "";
      comment.value.index = -1;
      comment.value.parent = -1;
      detailArticle.value.commentNum += 1;
    }
  });
}

function editComment() {
  changeCommentDetail(changeData.value).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      ElMessage.success("修改成功");
      getByArticleId(userId.value, articleId).then((response) => {
        let res = response.data;
        if (res.code == 200) {
          detailArticle.value.comments = res.data;
        }
      });
      changeVisible.value = false;
    }
  });
}

function handleDelete(id) {
  ElMessageBox.confirm("确认删除该评论吗?", "error", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      deleteComment(id, articleId).then((response) => {
        let res = response.data;
        if (res.code == 200) {
          ElMessage.success("删除成功");
          // 删除之后更新评论区
          getByArticleId(userId.value, articleId).then((response) => {
            let res = response.data;
            if (res.code == 200) {
              detailArticle.value.comments = res.data;
            }
            // 同理，删除评论后前端数据量显式减一，不用向后端发请求
            detailArticle.value.commentNum -= 1;
          });
        }
      });
    })
    .catch(() => {});
}

// 修改评论时数据的初始化
function handleChange(id, c) {
  changeVisible.value = true;
  changeData.value.id = id;
  changeData.value.content = c;
}
</script>

<style>
.item {
  display: flex;
  margin-left: 3%;
}
</style>
