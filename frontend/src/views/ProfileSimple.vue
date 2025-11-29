<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="profile-header">
        <el-avatar :size="100">{{ userInfo?.username?.charAt(0) || 'U' }}</el-avatar>
        <div class="profile-info">
          <h2>{{ userInfo?.username || '加载中...' }}</h2>
          <p class="user-email">{{ userInfo?.email || '' }}</p>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ followStats.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ followStats.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ postCount }}</span>
              <span class="stat-label">帖子</span>
            </div>
          </div>
        </div>
        <div class="profile-actions" v-if="isOwnProfile">
          <el-button type="primary" @click="router.push('/settings')">设置</el-button>
        </div>
        <div class="profile-actions" v-else>
          <el-button
            :type="isFollowing ? 'default' : 'primary'"
            @click="toggleFollow"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="帖子" name="posts">
        <div class="posts-list">
          <el-scrollbar style="height: calc(100vh - 400px)">
            <el-space direction="vertical" :size="20" style="width: 100%">
              <el-card
                v-for="post in userPosts"
                :key="post.id"
                shadow="hover"
                class="post-card"
                @click="goToDetail(post.id)"
              >
                <div class="post-content">
                  <h3 class="post-title">{{ post.title }}</h3>
                  <div class="post-body" v-html="formatContent(post.content)"></div>
                  <div class="post-meta">
                    <span class="post-time">{{ formatTime(post.createTime) }}</span>
                    <div class="post-stats">
                      <el-tag size="small">👍 {{ post.likeCount || 0 }}</el-tag>
                      <el-tag size="small" type="warning">⭐ {{ post.favoriteCount || 0 }}</el-tag>
                      <el-tag size="small" type="info">💬 {{ post.commentCount || 0 }}</el-tag>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-space>
          </el-scrollbar>
        </div>
        <el-pagination
          v-if="postTotal > 0"
          v-model:current-page="postPage"
          v-model:page-size="postPageSize"
          :total="postTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePostSizeChange"
          @current-change="handlePostPageChange"
          style="margin-top: 20px; justify-content: center"
        />
      </el-tab-pane>
      <el-tab-pane label="收藏" name="favorites" v-if="isOwnProfile">
        <div class="favorites-list">
          <el-scrollbar style="height: calc(100vh - 400px)">
            <el-space direction="vertical" :size="20" style="width: 100%">
              <el-card
                v-for="post in favoritePosts"
                :key="post.id"
                shadow="hover"
                class="post-card"
                @click="goToDetail(post.id)"
              >
                <div class="post-content">
                  <h3 class="post-title">{{ post.title }}</h3>
                  <div class="post-body" v-html="formatContent(post.content)"></div>
                </div>
              </el-card>
            </el-space>
          </el-scrollbar>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { getUserInfo } from "@/api/user";
import { getPostsByUserId } from "@/api/post";
import { getFollowCount, follow, unfollow, checkFollowing } from "@/api/relation";
import { getUserFavorites } from "@/api/relation";
import { getPostById } from "@/api/post";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const userId = computed(() => {
  return route.params.userId ? Number(route.params.userId) : userStore.userId;
});

const isOwnProfile = computed(() => {
  return userId.value === userStore.userId;
});

const userInfo = ref({});
const followStats = ref({ followingCount: 0, followerCount: 0 });
const isFollowing = ref(false);
const activeTab = ref("posts");
const userPosts = ref([]);
const postPage = ref(1);
const postPageSize = ref(20);
const postTotal = ref(0);
const postCount = ref(0);
const favoritePosts = ref([]);

onMounted(() => {
  loadUserInfo();
  loadFollowStats();
  loadUserPosts();
  if (isOwnProfile.value) {
    loadFavorites();
  }
});

async function loadUserInfo() {
  try {
    const response = await getUserInfo(userId.value);
    if (response.data.code === 200) {
      userInfo.value = response.data.data;
    }
  } catch (error) {
    console.error("加载用户信息失败:", error);
  }
}

async function loadFollowStats() {
  try {
    const response = await getFollowCount(userId.value);
    if (response.data.code === 200) {
      followStats.value = response.data.data;
    }
    
    // 检查是否关注
    if (!isOwnProfile.value && userStore.userId) {
      const followRes = await checkFollowing(userStore.userId, userId.value);
      if (followRes.data.code === 200) {
        isFollowing.value = followRes.data.data.isFollowing;
      }
    }
  } catch (error) {
    console.error("加载关注统计失败:", error);
  }
}

async function loadUserPosts() {
  try {
    const response = await getPostsByUserId(userId.value, postPage.value, postPageSize.value);
    if (response.data.code === 200) {
      userPosts.value = response.data.data || [];
      postTotal.value = userPosts.value.length;
      postCount.value = userPosts.value.length;
    }
  } catch (error) {
    console.error("加载用户帖子失败:", error);
  }
}

async function loadFavorites() {
  try {
    const response = await getUserFavorites(userId.value);
    if (response.data.code === 200) {
      const postIds = response.data.data.favorites || [];
      // 获取帖子详情
      const posts = [];
      for (const postId of postIds) {
        try {
          const postRes = await getPostById(postId);
          if (postRes.data.code === 200) {
            posts.push(postRes.data.data);
          }
        } catch (error) {
          console.error("加载帖子详情失败:", error);
        }
      }
      favoritePosts.value = posts;
    }
  } catch (error) {
    console.error("加载收藏列表失败:", error);
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
      followStats.value.followerCount = Math.max(0, followStats.value.followerCount - 1);
      ElMessage.success("取消关注");
    } else {
      await follow(userStore.userId, userId.value);
      isFollowing.value = true;
      followStats.value.followerCount = (followStats.value.followerCount || 0) + 1;
      ElMessage.success("关注成功");
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
  return content.replace(/\n/g, "<br>");
}

function handlePostSizeChange(val) {
  postPageSize.value = val;
  loadUserPosts();
}

function handlePostPageChange(val) {
  postPage.value = val;
  loadUserPosts();
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.profile-card {
  margin-bottom: 20px;
}

.profile-header {
  display: flex;
  gap: 30px;
  align-items: center;
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  margin: 0 0 10px 0;
}

.user-email {
  color: #909399;
  margin: 0 0 20px 0;
}

.profile-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.profile-actions {
  display: flex;
  gap: 10px;
}

.profile-tabs {
  margin-top: 20px;
}

.posts-list,
.favorites-list {
  min-height: 300px;
}

.post-card {
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.post-content {
  padding: 10px;
}

.post-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: bold;
}

.post-body {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  max-height: 150px;
  overflow: hidden;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.post-time {
  color: #909399;
  font-size: 12px;
}

.post-stats {
  display: flex;
  gap: 10px;
}
</style>

