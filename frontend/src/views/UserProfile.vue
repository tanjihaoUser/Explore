<template>
  <div class="user-profile-container">
    <div class="user-header">
      <el-avatar :size="80">{{ userInfo?.username?.charAt(0) || 'U' }}</el-avatar>
      <div class="user-info">
        <h2>{{ userInfo?.username || '匿名用户' }}</h2>
        <div class="user-stats">
          <div class="stat-item">
            <span class="stat-label">帖子数：</span>
            <span class="stat-value">{{ userStats.postCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">点赞总数：</span>
            <span class="stat-value">{{ userStats.totalLikeCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">收藏总数：</span>
            <span class="stat-value">{{ userStats.totalFavoriteCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">评论总数：</span>
            <span class="stat-value">{{ userStats.totalCommentCount || 0 }}</span>
          </div>
        </div>
      </div>
      <div class="user-actions" v-if="userInfo && userInfo.id !== userStore.userId">
        <el-button
          :type="isFollowing ? 'info' : 'primary'"
          @click="toggleFollow"
        >
          <el-icon><UserFilled /></el-icon>
          {{ isFollowing ? '已关注' : '关注' }}
        </el-button>
      </div>
    </div>

    <div class="posts-section">
      <h3>发布的帖子</h3>
      <div class="posts-container">
        <el-empty v-if="posts.length === 0 && !loading" description="该用户还没有发布任何帖子" />
        <el-scrollbar v-else style="height: calc(100vh - 350px)">
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
                  <div class="post-time">{{ formatTime(post.createdAt) }}</div>
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
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { UserFilled, ArrowUp, Star, ChatDotRound } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { getUserInfo } from "@/api/user";
import { getPostsByUserId } from "@/api/post";
import { follow, unfollow, checkFollowing } from "@/api/relation";
import { likePost, unlikePost, favoritePost, unfavoritePost } from "@/api/relation";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const userId = computed(() => route.params.userId);
const userInfo = ref(null);
const userStats = ref({
  postCount: 0,
  totalLikeCount: 0,
  totalFavoriteCount: 0,
  totalCommentCount: 0,
});
const posts = ref([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const loading = ref(false);
const isFollowing = ref(false);

onMounted(() => {
  loadUserInfo();
  loadPosts();
  checkFollowStatus();
});

async function loadUserInfo() {
  try {
    const response = await getUserInfo(userId.value);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    if (isSuccess) {
      userInfo.value = response.data.data;
    }
  } catch (error) {
    console.error("加载用户信息失败:", error);
  }
}

async function loadPosts() {
  loading.value = true;
  try {
    const response = await getPostsByUserId(userId.value, currentPage.value, pageSize.value, userStore.userId);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      const postList = response.data.data?.posts || response.data.data || [];
      posts.value = postList;
      total.value = response.data.data?.count || response.data.count || postList.length;
      
      // 计算用户统计数据
      if (postList.length > 0) {
        userStats.value = {
          postCount: total.value,
          totalLikeCount: postList.reduce((sum, post) => sum + (post.likeCount || 0), 0),
          totalFavoriteCount: postList.reduce((sum, post) => sum + (post.favoriteCount || 0), 0),
          totalCommentCount: postList.reduce((sum, post) => sum + (post.commentCount || 0), 0),
        };
      }
    } else {
      ElMessage.error(response.data.message || "加载帖子失败");
    }
  } catch (error) {
    console.error("加载帖子失败:", error);
    ElMessage.error("加载帖子失败");
  } finally {
    loading.value = false;
  }
}

async function checkFollowStatus() {
  if (!userStore.userId || userId.value === userStore.userId) {
    return;
  }
  
  try {
    const response = await checkFollowing(userStore.userId, userId.value);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    if (isSuccess) {
      isFollowing.value = response.data.data?.isFollowing || false;
    }
  } catch (error) {
    console.error("检查关注状态失败:", error);
  }
}

async function toggleFollow() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (isFollowing.value) {
      await unfollow(userStore.userId, userId.value);
      isFollowing.value = false;
      ElMessage.success("取消关注");
    } else {
      await follow(userStore.userId, userId.value);
      isFollowing.value = true;
      ElMessage.success("关注成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
    ElMessage.error("操作失败");
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
  const lines = content.split('\n');
  if (lines[0] && lines[0].trim().startsWith('#')) {
    lines.shift();
    if (lines[0] && lines[0].trim() === '') {
      lines.shift();
    }
  }
  const firstLine = lines.find(line => line.trim()) || '';
  const preview = firstLine.trim().substring(0, 100);
  return preview + (firstLine.trim().length > 100 ? '...' : '');
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
.user-profile-container {
  padding: 20px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 30px;
  background: white;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-info {
  flex: 1;
}

.user-info h2 {
  margin: 0 0 15px 0;
  font-size: 24px;
  color: #303133;
}

.user-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-right: 5px;
}

.stat-value {
  color: #303133;
  font-weight: bold;
  font-size: 16px;
}

.user-actions {
  flex-shrink: 0;
}

.posts-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.posts-section h3 {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: #303133;
}

.posts-container {
  min-height: 400px;
}

.posts-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
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
  justify-content: space-between;
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

