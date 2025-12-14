<template>
  <div class="post-detail-container">
    <el-card v-if="post" class="post-card">
      <template #header>
        <div class="post-header">
          <div class="post-author">
            <el-avatar :size="50">{{ post.username?.charAt(0) || 'U' }}</el-avatar>
            <div class="author-info">
              <div class="author-name">{{ post.username || '匿名用户' }}</div>
              <div class="post-time">{{ formatTime(post.createdAt || post.createTime) }}</div>
            </div>
          </div>
          <div class="post-actions" v-if="isOwnPost">
            <el-button type="primary" size="small" @click="handleEdit">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete">删除</el-button>
          </div>
        </div>
      </template>
      
      <div class="post-content">
        <h1 class="post-title">{{ post.title || extractTitle(post.content) }}</h1>
        <div class="post-body">
          <v-md-preview :text="post.content || ''" />
        </div>
      </div>
      
      <div class="post-footer">
        <el-button-group>
          <el-button
            :type="post.isLiked ? 'danger' : 'default'"
            @click="toggleLike"
          >
            <el-icon><ArrowUp /></el-icon>
            {{ post.likeCount || 0 }}
          </el-button>
          <el-button
            :type="post.isFavorited ? 'warning' : 'default'"
            @click="toggleFavorite"
          >
            <el-icon><Star /></el-icon>
            {{ post.favoriteCount || 0 }}
          </el-button>
          <el-button
            type="info"
            disabled
          >
            <el-icon><View /></el-icon>
            {{ viewCount }}
          </el-button>
        </el-button-group>
      </div>
    </el-card>

    <el-card class="comments-card">
      <template #header>
        <div class="comments-header">
          <span>评论 ({{ commentCount }})</span>
        </div>
      </template>
      
      <div class="comment-form">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
        />
        <div class="comment-actions">
          <el-button type="primary" @click="handleAddComment" :disabled="!newComment.trim()">
            发表评论
          </el-button>
        </div>
      </div>
      
      <div class="comments-list">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="comment-item"
        >
          <div class="comment-header">
            <el-avatar :size="40">{{ comment.username?.charAt(0) || 'U' }}</el-avatar>
            <div class="comment-info">
              <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt || comment.createTime) }}</span>
            </div>
            <div class="comment-actions" v-if="comment.userId === userStore.userId">
              <el-button type="danger" size="small" text @click="handleDeleteComment(comment.id)">
                删除
              </el-button>
            </div>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
      </div>
      
      <el-pagination
        v-if="commentTotal > 0"
        v-model:current-page="commentPage"
        v-model:page-size="commentPageSize"
        :total="commentTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleCommentSizeChange"
        @current-change="handleCommentPageChange"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getPostById, deletePost } from "@/api/post";
import { likePost, unlikePost, favoritePost, unfavoritePost, checkLiked, checkFavorited } from "@/api/relation";
import { getLikeCount, getFavoriteCount } from "@/api/relation";
import { getPostComments, createComment, deleteComment, getCommentCount } from "@/api/comment";
import { getPostViewStatistics } from "@/api/statistics";
import { recordBrowse } from "@/api/browseHistory";
import { ElMessage, ElMessageBox } from "element-plus";
import { View } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const postId = computed(() => Number(route.params.postId));
const post = ref(null);
const isOwnPost = computed(() => {
  return post.value && post.value.userId === userStore.userId;
});

const comments = ref([]);
const commentPage = ref(1);
const commentPageSize = ref(20);
const commentTotal = ref(0);
const commentCount = ref(0);
const newComment = ref("");
const viewCount = ref(0);

onMounted(() => {
  loadPost();
  loadComments();
  loadViewCount();
  recordBrowseHistory();
});

async function loadPost() {
  try {
    // 传递 userId 参数，后端会自动记录浏览历史
    const response = await getPostById(postId.value, userStore.userId);
    // 兼容两种响应格式
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      post.value = response.data.data;
      
      // 如果没有 title 字段，从 content 中提取
      if (!post.value.title && post.value.content) {
        post.value.title = extractTitle(post.value.content);
      }
      
      // 加载用户信息（如果没有 username）
      if (post.value.userId && !post.value.username) {
        try {
          const { getUserInfo } = await import("@/api/user");
          const userRes = await getUserInfo(post.value.userId);
          const userSuccess = (userRes.data.code === 200) || (userRes.data.success === true);
          if (userSuccess) {
            post.value.username = userRes.data.data?.username || '匿名用户';
          }
        } catch (error) {
          console.error("加载用户信息失败:", error);
          post.value.username = '匿名用户';
        }
      }
      
      await checkPostStatus();
    } else {
      ElMessage.error(response.data.message || "加载帖子失败");
    }
  } catch (error) {
    console.error("加载帖子失败:", error);
    ElMessage.error("加载帖子失败");
  }
}

function extractTitle(content) {
  if (!content) return "无标题";
  const lines = content.split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith('#')) {
      return trimmed.replace(/^#+\s*/, '');
    }
    if (trimmed) {
      return trimmed.substring(0, 50);
    }
  }
  return "无标题";
}

async function checkPostStatus() {
  if (!userStore.userId || !post.value) return;
  
  try {
    const [likedRes, favoritedRes, likeCountRes, favoriteCountRes, commentCountRes] = await Promise.all([
      checkLiked(userStore.userId, postId.value),
      checkFavorited(userStore.userId, postId.value),
      getLikeCount(postId.value),
      getFavoriteCount(postId.value),
      getCommentCount(postId.value),
    ]);
    
    // 兼容两种响应格式
    const getData = (res) => {
      const isSuccess = (res.data.code === 200) || (res.data.success === true);
      return isSuccess ? (res.data.data || res.data) : {};
    };
    
    const likedData = getData(likedRes);
    const favoritedData = getData(favoritedRes);
    const likeCountData = getData(likeCountRes);
    const favoriteCountData = getData(favoriteCountRes);
    const commentCountData = getData(commentCountRes);
    
    post.value.isLiked = likedData.isLiked || false;
    post.value.isFavorited = favoritedData.isFavorited || false;
    post.value.likeCount = likeCountData.likeCount || 0;
    post.value.favoriteCount = favoriteCountData.favoriteCount || 0;
    commentCount.value = commentCountData.count || commentCountData.commentCount || 0;
  } catch (error) {
    console.error("检查帖子状态失败:", error);
  }
}

async function loadComments() {
  try {
    const response = await getPostComments(postId.value, commentPage.value, commentPageSize.value);
    // 兼容两种响应格式
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      comments.value = response.data.data?.comments || response.data.data || [];
      commentTotal.value = response.data.data?.count || response.data.count || 0;
      
      // 为每个评论加载用户信息（如果没有 username）
      for (const comment of comments.value) {
        if (comment.userId && !comment.username) {
          try {
            const { getUserInfo } = await import("@/api/user");
            const userRes = await getUserInfo(comment.userId);
            const userSuccess = (userRes.data.code === 200) || (userRes.data.success === true);
            if (userSuccess) {
              comment.username = userRes.data.data?.username || '匿名用户';
            }
          } catch (error) {
            console.error("加载用户信息失败:", error);
            comment.username = '匿名用户';
          }
        }
      }
    } else {
      ElMessage.error(response.data.message || "加载评论失败");
    }
  } catch (error) {
    console.error("加载评论失败:", error);
  }
}

async function toggleLike() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (post.value.isLiked) {
      await unlikePost(userStore.userId, postId.value);
      post.value.isLiked = false;
      post.value.likeCount = Math.max(0, (post.value.likeCount || 0) - 1);
      ElMessage.success("取消点赞");
    } else {
      await likePost(userStore.userId, postId.value);
      post.value.isLiked = true;
      post.value.likeCount = (post.value.likeCount || 0) + 1;
      ElMessage.success("点赞成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
  }
}

async function toggleFavorite() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (post.value.isFavorited) {
      await unfavoritePost(userStore.userId, postId.value);
      post.value.isFavorited = false;
      post.value.favoriteCount = Math.max(0, (post.value.favoriteCount || 0) - 1);
      ElMessage.success("取消收藏");
    } else {
      await favoritePost(userStore.userId, postId.value);
      post.value.isFavorited = true;
      post.value.favoriteCount = (post.value.favoriteCount || 0) + 1;
      ElMessage.success("收藏成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
  }
}

async function handleAddComment() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  if (!newComment.value.trim()) {
    ElMessage.warning("请输入评论内容");
    return;
  }
  
  try {
    const response = await createComment({
      userId: userStore.userId,
      postId: postId.value,
      content: newComment.value,
    });
    
    // 兼容两种响应格式
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      ElMessage.success("评论成功");
      newComment.value = "";
      commentCount.value++;
      loadComments();
    } else {
      ElMessage.error(response.data.message || "评论失败");
    }
  } catch (error) {
    console.error("发表评论失败:", error);
  }
}

async function handleDeleteComment(commentId) {
  try {
    ElMessageBox.confirm("确认删除这条评论吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(async () => {
      const response = await deleteComment(commentId, userStore.userId);
      const isSuccess = (response.data.code === 200) || (response.data.success === true);
      
      if (isSuccess) {
        ElMessage.success("删除成功");
        commentCount.value = Math.max(0, commentCount.value - 1);
        loadComments();
      } else {
        ElMessage.error(response.data.message || "删除失败");
      }
    });
  } catch (error) {
    console.error("删除评论失败:", error);
  }
}

async function handleDelete() {
  try {
    ElMessageBox.confirm("确认删除这条帖子吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(async () => {
      const response = await deletePost(userStore.userId, postId.value);
      const isSuccess = (response.data.code === 200) || (response.data.success === true);
      
      if (isSuccess) {
        ElMessage.success("删除成功");
        router.push("/home");
      } else {
        ElMessage.error(response.data.message || "删除失败");
      }
    });
  } catch (error) {
    console.error("删除帖子失败:", error);
  }
}

function handleEdit() {
  router.push(`/create-post?edit=${postId.value}`);
}

function formatTime(time) {
  if (!time) return "";
  const date = new Date(time);
  return date.toLocaleString("zh-CN");
}

function handleCommentSizeChange(val) {
  commentPageSize.value = val;
  loadComments();
}

function handleCommentPageChange(val) {
  commentPage.value = val;
  loadComments();
}

async function loadViewCount() {
  try {
    const response = await getPostViewStatistics(postId.value, 24);
    if (response.data.code === 200) {
      const data = response.data.data;
      // 计算总浏览量
      viewCount.value = data.total || 0;
    }
  } catch (error) {
    console.error("加载浏览量失败:", error);
  }
}

async function recordBrowseHistory() {
  if (!userStore.userId || !postId.value) {
    return;
  }
  
  try {
    await recordBrowse(userStore.userId, postId.value);
  } catch (error) {
    // 静默失败，不影响用户体验
    console.error("记录浏览历史失败:", error);
  }
}
</script>

<style scoped>
.post-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.post-card {
  margin-bottom: 20px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-author {
  display: flex;
  gap: 15px;
  align-items: center;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: bold;
  font-size: 16px;
}

.post-time {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.post-content {
  margin: 20px 0;
}

.post-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  font-weight: bold;
}

.post-body {
  line-height: 1.8;
  color: #606266;
}

.post-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.comments-card {
  margin-top: 20px;
}

.comments-header {
  font-size: 18px;
  font-weight: bold;
}

.comment-form {
  margin-bottom: 30px;
}

.comment-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.comments-list {
  margin-top: 20px;
}

.comment-item {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.comment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-author {
  font-weight: bold;
}

.comment-time {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.comment-content {
  color: #606266;
  line-height: 1.6;
  margin-left: 50px;
}
</style>

