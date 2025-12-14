<template>
  <div class="discover-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>发现</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshRecommendations">刷新推荐</el-button>
            <el-button @click="clearHistory">清除历史</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="recommendedUsers.length > 0" class="users-grid">
        <el-card
          v-for="user in recommendedUsers"
          :key="user.id"
          shadow="hover"
          class="user-card"
        >
          <div class="user-content">
            <el-avatar :size="80">{{ user.username?.charAt(0) || 'U' }}</el-avatar>
            <div class="user-info">
              <h3 class="user-name">{{ user.username || '匿名用户' }}</h3>
              <div class="user-stats">
                <span>粉丝: {{ user.followerCount || 0 }}</span>
                <span>关注: {{ user.followingCount || 0 }}</span>
              </div>
            </div>
            <div class="user-actions">
              <el-button
                v-if="!user.isFollowing"
                type="primary"
                @click="handleFollow(user.id)"
              >
                关注
              </el-button>
              <el-button
                v-else
                @click="handleUnfollow(user.id)"
              >
                已关注
              </el-button>
              <el-button
                type="info"
                @click="goToProfile(user.id)"
              >
                查看主页
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无推荐用户" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { ElMessage, ElMessageBox } from "element-plus";
import { getRecommendedUsers, clearRecommendedHistory } from "@/api/recommendation";
import { follow, unfollow } from "@/api/relation";
import { getUserInfo } from "@/api/user";

const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const recommendedUsers = ref([]);

onMounted(() => {
  loadRecommendations();
});

async function loadRecommendations() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  loading.value = true;
  try {
    const response = await getRecommendedUsers(userStore.userId, 12);
    if (response.data.code === 200) {
      const userIds = response.data.data?.recommendedUserIds || [];
      
      // 加载用户详细信息
      const userPromises = userIds.map(async (userId) => {
        try {
          const userRes = await getUserInfo(userId);
          if (userRes.data.code === 200) {
            const user = userRes.data.data;
            // 检查是否已关注
            const { checkFollowing } = await import("@/api/relation");
            const followRes = await checkFollowing(userStore.userId, userId);
            user.isFollowing = followRes.data.data?.isFollowing || false;
            
            // 获取关注数和粉丝数
            const { getFollowCount } = await import("@/api/relation");
            const countRes = await getFollowCount(userId);
            if (countRes.data.code === 200) {
              const countData = countRes.data.data;
              user.followerCount = countData.followerCount || 0;
              user.followingCount = countData.followingCount || 0;
            }
            
            return user;
          }
        } catch (error) {
          console.error(`加载用户信息失败: userId=${userId}`, error);
          return null;
        }
      });

      const users = await Promise.all(userPromises);
      recommendedUsers.value = users.filter((user) => user !== null);
    } else {
      ElMessage.error(response.data.message || "加载推荐用户失败");
    }
  } catch (error) {
    console.error("加载推荐用户失败:", error);
    ElMessage.error("加载推荐用户失败");
  } finally {
    loading.value = false;
  }
}

async function refreshRecommendations() {
  await loadRecommendations();
  ElMessage.success("推荐已刷新");
}

async function clearHistory() {
  try {
    ElMessageBox.confirm("确认清除推荐历史吗？清除后可以重新看到之前推荐过的用户。", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(async () => {
      const response = await clearRecommendedHistory(userStore.userId);
      if (response.data.code === 200) {
        ElMessage.success("清除历史成功");
        await loadRecommendations();
      }
    });
  } catch (error) {
    console.error("清除推荐历史失败:", error);
  }
}

async function handleFollow(userId) {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  try {
    const response = await follow(userStore.userId, userId);
    if (response.data.code === 200 && response.data.data?.success) {
      ElMessage.success("关注成功");
      const user = recommendedUsers.value.find((u) => u.id === userId);
      if (user) {
        user.isFollowing = true;
        user.followerCount = (user.followerCount || 0) + 1;
      }
    } else {
      ElMessage.error(response.data.message || "关注失败");
    }
  } catch (error) {
    console.error("关注失败:", error);
    ElMessage.error("关注失败");
  }
}

async function handleUnfollow(userId) {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  try {
    const response = await unfollow(userStore.userId, userId);
    if (response.data.code === 200 && response.data.data?.success) {
      ElMessage.success("取消关注成功");
      const user = recommendedUsers.value.find((u) => u.id === userId);
      if (user) {
        user.isFollowing = false;
        user.followerCount = Math.max(0, (user.followerCount || 0) - 1);
      }
    } else {
      ElMessage.error(response.data.message || "取消关注失败");
    }
  } catch (error) {
    console.error("取消关注失败:", error);
    ElMessage.error("取消关注失败");
  }
}

function goToProfile(userId) {
  router.push(`/user/${userId}`);
}
</script>

<style scoped>
.discover-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-container {
  padding: 20px;
}

.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.user-card {
  transition: transform 0.3s;
}

.user-card:hover {
  transform: translateY(-5px);
}

.user-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  padding: 20px;
}

.user-info {
  text-align: center;
}

.user-name {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: bold;
}

.user-stats {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #909399;
}

.user-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  justify-content: center;
}
</style>

