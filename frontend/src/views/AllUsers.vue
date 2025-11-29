<template>
  <div class="all-users-container">
    <div class="header">
      <h2>所有用户</h2>
      <p class="subtitle">按帖子数、点赞、收藏、评论加权排序</p>
    </div>

    <div class="users-container">
      <el-empty v-if="users.length === 0 && !loading" description="暂无用户数据" />
      <el-scrollbar v-else style="height: calc(100vh - 200px)">
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
import { Trophy, Medal, Star, UserFilled } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { getUserRanking } from "@/api/user";
import { follow, unfollow } from "@/api/relation";

const router = useRouter();
const userStore = useUserStore();

const users = ref([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const loading = ref(false);

onMounted(() => {
  loadUsers();
});

async function loadUsers() {
  loading.value = true;
  try {
    const response = await getUserRanking(currentPage.value, pageSize.value, userStore.userId);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      users.value = response.data.data?.users || [];
      total.value = response.data.data?.count || users.value.length;
    } else {
      ElMessage.error(response.data.message || "加载用户列表失败");
    }
  } catch (error) {
    console.error("加载用户列表失败:", error);
    ElMessage.error("加载用户列表失败");
  } finally {
    loading.value = false;
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

function formatScore(score) {
  if (!score) return "0";
  return score.toFixed(1);
}

function handleSizeChange(val) {
  pageSize.value = val;
  loadUsers();
}

function handlePageChange(val) {
  currentPage.value = val;
  loadUsers();
}
</script>

<style scoped>
.all-users-container {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
  text-align: center;
}

.header h2 {
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

