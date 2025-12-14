<template>
  <div class="home-container">
    <div class="home-header">
      <el-radio-group v-model="currentView" @change="handleViewChange">
        <el-radio-button label="my">我的主页</el-radio-button>
        <el-radio-button label="users">所有用户</el-radio-button>
        <el-radio-button label="create">发布新帖</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 我的主页内容 -->
    <div v-if="currentView === 'my'" class="posts-container">
      <el-empty v-if="posts.length === 0 && !loading" description="还没有发布任何帖子" />
      <el-scrollbar v-else style="height: calc(100vh - 200px)">
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
                <el-button disabled circle>
                  <el-icon><View /></el-icon>
                  <span style="margin-left: 5px">{{ post.viewCount || 0 }}</span>
                </el-button>
              </div>
              <div class="post-actions" v-if="isOwnPost(post)">
                <el-button type="primary" size="small" @click="handleEdit(post.id)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete(post)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-scrollbar>
    </div>

    <!-- 所有用户内容 -->
    <div v-if="currentView === 'users'" class="users-view-container">
      <div class="users-header">
        <h2>所有用户</h2>
        <p class="subtitle">按帖子数、点赞、收藏、评论加权排序</p>
      </div>
      <div class="users-container">
        <el-empty v-if="users.length === 0 && !usersLoading" description="暂无用户数据" />
        <el-scrollbar v-else style="height: calc(100vh - 250px)">
          <div class="users-wrapper">
            <el-card
              v-for="(user, index) in users"
              :key="user.userId"
              shadow="hover"
              class="user-card"
              @click="goToUserProfile(user.userId)"
            >
              <div class="user-content">
                <div class="user-info">
                  <div class="user-avatar">
                    <el-avatar :size="60">{{ user.username?.charAt(0) || 'U' }}</el-avatar>
                    <div class="rank-badge" v-if="index < 3">
                      <el-icon v-if="index === 0"><Trophy /></el-icon>
                      <el-icon v-else-if="index === 1"><Medal /></el-icon>
                      <el-icon v-else><Star /></el-icon>
                    </div>
                  </div>
                  <div class="user-details">
                    <h3 class="username">{{ user.username || '匿名用户' }}</h3>
                    <div class="user-stats">
                      <div class="stat-item">
                        <span class="stat-label">帖子数：</span>
                        <span class="stat-value">{{ user.postCount || 0 }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">点赞总数：</span>
                        <span class="stat-value">{{ user.totalLikeCount || 0 }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">收藏总数：</span>
                        <span class="stat-value">{{ user.totalFavoriteCount || 0 }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">评论总数：</span>
                        <span class="stat-value">{{ user.totalCommentCount || 0 }}</span>
                      </div>
                    </div>
                    <div class="score-info">
                      <span class="score-label">综合评分：</span>
                      <span class="score-value">{{ formatScore(user.score) }}</span>
                    </div>
                  </div>
                </div>
                <div class="user-actions" @click.stop>
                  <el-button
                    v-if="user.userId !== userStore.userId"
                    :type="user.isFollowing ? 'info' : 'primary'"
                    @click="toggleFollow(user)"
                  >
                    <el-icon><UserFilled /></el-icon>
                    {{ user.isFollowing ? '已关注' : '关注' }}
      </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </el-scrollbar>
      </div>
      <el-pagination
        v-if="usersTotal > 0"
        v-model:current-page="usersCurrentPage"
        v-model:page-size="usersPageSize"
        :total="usersTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleUsersSizeChange"
        @current-change="handleUsersPageChange"
        style="margin-top: 20px; justify-content: center"
      />
    </div>

    <!-- 发布新帖 - 直接跳转 -->
    <div v-if="currentView === 'create'">
      <!-- 这个视图会立即跳转到创建帖子页面 -->
    </div>

    <!-- 分页（仅在我的主页显示） -->
    <el-pagination
      v-if="currentView === 'my' && total > 0"
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
import { ref, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Trophy, Medal, Star, UserFilled, ArrowUp, ChatDotRound, Edit, Delete, View } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { getPostsByUserId, deletePost } from "@/api/post";
import { likePost, unlikePost, favoritePost, unfavoritePost } from "@/api/relation";
import { getUserRanking } from "@/api/user";
import { follow, unfollow } from "@/api/relation";
import { getPostViewStatistics } from "@/api/statistics";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const currentView = ref("my"); // 'my', 'users', 'create'
const posts = ref([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const loading = ref(false);

// 用户列表相关
const users = ref([]);
const usersCurrentPage = ref(1);
const usersPageSize = ref(20);
const usersTotal = ref(0);
const usersLoading = ref(false);

onMounted(() => {
  // 根据路由参数设置当前视图
  if (route.query.view === 'users') {
    currentView.value = 'users';
  } else if (route.query.view === 'create') {
    currentView.value = 'create';
    router.push('/create-post');
  } else {
    currentView.value = 'my';
  if (userStore.userId) {
    loadPosts();
  } else {
    ElMessage.warning("请先登录");
    }
  }
});

// 监听视图变化
watch(currentView, (newView) => {
  if (newView === 'create') {
    router.push('/create-post');
  } else if (newView === 'users') {
    // 更新URL但不重新加载组件
    router.replace({ path: '/home', query: { view: 'users' } });
    loadUsers();
  } else {
    router.replace({ path: '/home' });
    if (userStore.userId) {
      loadPosts();
    }
  }
});

async function loadPosts() {
  if (!userStore.userId) {
    return;
  }
  
  loading.value = true;
  try {
    // 传递当前用户ID，后端会返回点赞收藏状态和统计数据
    const response = await getPostsByUserId(userStore.userId, currentPage.value, pageSize.value, userStore.userId);
    // 兼容两种响应格式
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      const postList = response.data.data?.posts || response.data.data || [];
      
      // 后端已经返回了所有需要的数据（点赞收藏状态、统计数据、用户名等）
      // 直接使用返回的数据，无需额外请求
      posts.value = postList;
      total.value = response.data.data?.count || response.data.count || postList.length;
      
      // 为每个帖子加载浏览量（异步，不阻塞）
      loadViewCountsForPosts(postList);
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

function isOwnPost(post) {
  return post.userId && userStore.userId && String(post.userId) === String(userStore.userId);
}

function handleEdit(postId) {
  router.push(`/create-post?edit=${postId}`);
}

async function handleDelete(post) {
  try {
    await ElMessageBox.confirm(
      `确定要删除帖子"${extractTitle(post.content)}"吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    
    // 执行删除
    await deletePost(userStore.userId, post.id);
    ElMessage.success("删除成功");
    
    // 重新加载帖子列表
    await loadPosts();
  } catch (error) {
    if (error !== 'cancel') {
      console.error("删除帖子失败:", error);
      ElMessage.error("删除失败");
    }
  }
}

async function loadUsers() {
  usersLoading.value = true;
  try {
    const response = await getUserRanking(usersCurrentPage.value, usersPageSize.value, userStore.userId);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      users.value = response.data.data?.users || [];
      usersTotal.value = response.data.data?.count || users.value.length;
    } else {
      ElMessage.error(response.data.message || "加载用户列表失败");
    }
  } catch (error) {
    console.error("加载用户列表失败:", error);
    ElMessage.error("加载用户列表失败");
  } finally {
    usersLoading.value = false;
  }
}

async function toggleFollow(user) {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    if (user.isFollowing) {
      await unfollow(userStore.userId, user.userId);
      user.isFollowing = false;
      ElMessage.success("取消关注");
    } else {
      await follow(userStore.userId, user.userId);
      user.isFollowing = true;
      ElMessage.success("关注成功");
    }
  } catch (error) {
    console.error("操作失败:", error);
    ElMessage.error("操作失败");
  }
}

function goToUserProfile(userId) {
  router.push(`/user/${userId}`);
}

async function loadViewCountsForPosts(postList) {
  // 异步加载每个帖子的浏览量，不阻塞主流程
  postList.forEach(async (post) => {
    try {
      const response = await getPostViewStatistics(post.id, 24);
      if (response.data.code === 200) {
        const data = response.data.data;
        post.viewCount = data.total || 0;
      }
    } catch (error) {
      console.error(`加载帖子浏览量失败: postId=${post.id}`, error);
      post.viewCount = 0;
    }
  });
}

function formatScore(score) {
  if (!score) return "0";
  return score.toFixed(1);
}

function handleViewChange() {
  // 视图切换时的处理
  if (currentView.value === 'my' && userStore.userId) {
    currentPage.value = 1;
    loadPosts();
  } else if (currentView.value === 'users') {
    usersCurrentPage.value = 1;
    loadUsers();
  }
}

function handleUsersSizeChange(val) {
  usersPageSize.value = val;
  loadUsers();
}

function handleUsersPageChange(val) {
  usersCurrentPage.value = val;
  loadUsers();
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
.home-container {
  padding: 20px;
}

.home-header {
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

.post-actions {
  display: flex;
  gap: 10px;
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

/* 用户列表样式 */
.users-view-container {
  padding: 0;
}

.users-header {
  margin-bottom: 20px;
  text-align: center;
}

.users-header h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.users-container {
  min-height: 400px;
}

.users-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
}

.user-card {
  width: 80%;
  cursor: pointer;
  transition: all 0.3s;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.user-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.user-avatar {
  position: relative;
}

.rank-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.user-details {
  flex: 1;
}

.username {
  margin: 0 0 15px 0;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.user-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  color: #303133;
  font-weight: bold;
  font-size: 14px;
}

.score-info {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.score-label {
  color: #909399;
  font-size: 14px;
}

.score-value {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;
}

.user-actions {
  flex-shrink: 0;
}
</style>

