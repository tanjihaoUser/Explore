<template>
  <div class="timeline-container">
    <div class="timeline-header">
      <el-radio-group v-model="timelineType" @change="handleTypeChange">
        <el-radio-button label="my">我的时间线</el-radio-button>
        <el-radio-button label="global">全局时间线</el-radio-button>
        <el-radio-button label="user">用户时间线</el-radio-button>
      </el-radio-group>
    </div>

    <div class="posts-container">
      <el-scrollbar style="height: calc(100vh - 200px)">
        <div class="posts-wrapper">
          <el-card
            v-for="post in posts"
            :key="post.id"
            shadow="hover"
            class="post-card"
            @click="goToDetail(post.id)"
          >
            <template #header>
              <div class="post-header">
                <div class="post-author">
                  <el-avatar :size="40">{{ post.username?.charAt(0) || 'U' }}</el-avatar>
                  <span class="author-name">{{ post.username || '匿名用户' }}</span>
                </div>
                <div class="post-time">{{ formatTime(post.createdAt || post.createTime) }}</div>
              </div>
            </template>
            <div class="post-content">
              <h3 class="post-title">{{ extractTitle(post.content) }}</h3>
              <div class="post-body" v-html="formatContent(post.content)"></div>
            </div>
            <div class="post-footer" @click.stop>
              <div class="action-buttons">
                <el-badge :value="post.likeCount || 0" :hidden="(post.likeCount || 0) === 0" class="badge-item">
                  <el-button
                    :type="post.isLiked ? 'danger' : 'default'"
                    @click="toggleLike(post)"
                    circle
                  >
                    <el-icon><ArrowUp /></el-icon>
                  </el-button>
                </el-badge>
                <el-badge :value="post.favoriteCount || 0" :hidden="(post.favoriteCount || 0) === 0" class="badge-item">
                  <el-button
                    :type="post.isFavorited ? 'warning' : 'default'"
                    @click="toggleFavorite(post)"
                    circle
                  >
                    <el-icon><Star /></el-icon>
                  </el-button>
                </el-badge>
                <el-badge :value="post.commentCount || 0" :hidden="(post.commentCount || 0) === 0" class="badge-item">
                  <el-button @click="goToDetail(post.id)" circle>
                    <el-icon><ChatDotRound /></el-icon>
                  </el-button>
                </el-badge>
              </div>
            </div>
          </el-card>
        </div>
      </el-scrollbar>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { getMyTimeline, getGlobalTimeline } from "@/api/ranking";
import { getPostsByUserId } from "@/api/post";
import { likePost, unlikePost, favoritePost, unfavoritePost } from "@/api/relation";

const router = useRouter();
const userStore = useUserStore();

const timelineType = ref("my");
const posts = ref([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

onMounted(() => {
  loadPosts();
});

async function loadPosts() {
  try {
    let response;
    if (timelineType.value === "my") {
      if (!userStore.userId) {
        ElMessage.warning("请先登录");
        return;
      }
      // 传递当前用户ID，后端会返回点赞收藏状态
      response = await getMyTimeline(userStore.userId, currentPage.value, pageSize.value, userStore.userId);
    } else if (timelineType.value === "global") {
      // 传递当前用户ID（如果已登录）
      response = await getGlobalTimeline(currentPage.value, pageSize.value, userStore.userId || null);
    } else {
      // 用户时间线，使用 /api/posts/user/{userId} 接口
      if (!userStore.userId) {
        ElMessage.warning("请先登录");
        return;
      }
      // 传递当前用户ID，后端会返回点赞收藏状态
      response = await getPostsByUserId(userStore.userId, currentPage.value, pageSize.value, userStore.userId);
    }
    
    // 兼容两种响应格式
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      // 全局时间线的数据在 response.data.data.posts 中
      // 用户时间线的数据可能在 response.data.data.posts 或 response.data.data 中
      let postList = [];
      if (timelineType.value === "global") {
        postList = response.data.data?.posts || [];
      } else {
        postList = response.data.data?.posts || response.data.data || [];
      }
      
      // 后端已经返回了所有需要的数据（点赞收藏状态、统计数据、用户名等）
      // 直接使用返回的数据，无需额外请求
      posts.value = postList;
      total.value = response.data.data?.count || response.data.count || postList.length;
    } else {
      ElMessage.error(response.data.message || "加载时间线失败");
    }
  } catch (error) {
    console.error("加载时间线失败:", error);
    ElMessage.error("加载时间线失败");
  }
}

async function toggleLike(post) {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (post.isLiked) {
      await unlikePost(userStore.userId, post.id);
      post.isLiked = false;
      post.likeCount = Math.max(0, (post.likeCount || 0) - 1);
      ElMessage.success("取消点赞");
    } else {
      await likePost(userStore.userId, post.id);
      post.isLiked = true;
      post.likeCount = (post.likeCount || 0) + 1;
      ElMessage.success("点赞成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
  }
}

async function toggleFavorite(post) {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (post.isFavorited) {
      await unfavoritePost(userStore.userId, post.id);
      post.isFavorited = false;
      post.favoriteCount = Math.max(0, (post.favoriteCount || 0) - 1);
      ElMessage.success("取消收藏");
    } else {
      await favoritePost(userStore.userId, post.id);
      post.isFavorited = true;
      post.favoriteCount = (post.favoriteCount || 0) + 1;
      ElMessage.success("收藏成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
  }
}

function goToDetail(postId) {
  router.push(`/post/${postId}`);
}

function formatTime(time) {
  if (!time) return "";
  const date = new Date(time);
  return date.toLocaleString("zh-CN");
}

function formatContent(content) {
  if (!content) return "";
  // 提取标题后的内容（去掉第一行标题）
  const lines = content.split('\n');
  // 如果第一行是标题（以#开头），则跳过
  if (lines[0] && lines[0].trim().startsWith('#')) {
    lines.shift();
    // 如果第二行是空行，也跳过
    if (lines[0] && lines[0].trim() === '') {
      lines.shift();
    }
  }
  // 只保留第一行，如果有多行则用...替代
  const firstLine = lines.find(line => line.trim()) || '';
  const preview = firstLine.trim().substring(0, 100);
  return preview + (firstLine.trim().length > 100 ? '...' : '');
}

function extractTitle(content) {
  if (!content) return "无标题";
  // 从 Markdown 内容中提取标题
  const lines = content.split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    // 匹配 Markdown 标题格式 # 标题 或 ## 标题
    if (trimmed.startsWith('#')) {
      return trimmed.replace(/^#+\s*/, '');
    }
    // 如果第一行不是标题，返回第一行作为标题
    if (trimmed) {
      return trimmed.substring(0, 50);
    }
  }
  return "无标题";
}

function handleTypeChange() {
  currentPage.value = 1;
  loadPosts();
}

function handleSizeChange(val) {
  pageSize.value = val;
  loadPosts();
}

function handlePageChange(val) {
  currentPage.value = val;
  loadPosts();
}
</script>

<style scoped>
.timeline-container {
  padding: 20px;
}

.timeline-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.posts-container {
  min-height: 400px;
}

.posts-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
}

.post-card {
  width: 80%;
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-name {
  font-weight: bold;
}

.post-time {
  color: #909399;
  font-size: 12px;
}

.post-content {
  margin: 15px 0;
}

.post-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: bold;
}

.post-body {
  color: #606266;
  line-height: 1.6;
  max-height: 200px;
  overflow: hidden;
}

.post-footer {
  margin-top: 15px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 15px;
  align-items: center;
}

.action-buttons .el-button {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-item {
  position: relative;
}

.badge-item :deep(.el-badge__content) {
  border-radius: 4px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 4px;
  font-size: 12px;
}
</style>

