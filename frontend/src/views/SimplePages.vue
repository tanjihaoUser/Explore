<template>
  <div class="simple-page-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>{{ pageTitle }}</h2>
        </div>
      </template>
      
      <div class="page-content">
        <el-empty v-if="items.length === 0" :description="emptyDescription" />
        <div v-else>
          <el-scrollbar style="height: calc(100vh - 300px)">
            <el-space direction="vertical" :size="20" style="width: 100%">
              <el-card
                v-for="item in items"
                :key="item.id"
                shadow="hover"
                class="item-card"
                @click="handleItemClick(item)"
              >
                <div class="item-content">
                  <slot :item="item">
                    <div class="item-title">{{ item.title || item.username || item.name }}</div>
                    <div class="item-meta">{{ formatTime(item.createTime || item.updateTime) }}</div>
                  </slot>
                </div>
              </el-card>
            </el-space>
          </el-scrollbar>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { getFollowing, getFollowers } from "@/api/relation";
import { getUserFavorites } from "@/api/relation";
import { getUserInfo } from "@/api/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const pageType = computed(() => {
  if (route.name === "Following") return "following";
  if (route.name === "Followers") return "followers";
  if (route.name === "Favorites") return "favorites";
  return "unknown";
});

const pageTitle = computed(() => {
  const titles = {
    following: "关注列表",
    followers: "粉丝列表",
    favorites: "我的收藏",
    settings: "设置",
    search: "搜索",
  };
  return titles[pageType.value] || "页面";
});

const emptyDescription = computed(() => {
  const descriptions = {
    following: "还没有关注任何人",
    followers: "还没有粉丝",
    favorites: "还没有收藏任何内容",
  };
  return descriptions[pageType.value] || "暂无数据";
});

const items = ref([]);

onMounted(() => {
  loadData();
});

async function loadData() {
  const userId = userStore.userId;
  if (!userId) {
    ElMessage.warning("请先登录");
    return;
  }
  
  try {
    let response;
    if (pageType.value === "following") {
      response = await getFollowing(userId);
      if (response.data.code === 200) {
        const userIds = response.data.data.following || [];
        // 获取用户详情
        const users = [];
        for (const id of userIds) {
          try {
            const userRes = await getUserInfo(id);
            if (userRes.data.code === 200) {
              users.push(userRes.data.data);
            }
          } catch (error) {
            console.error("加载用户信息失败:", error);
          }
        }
        items.value = users;
      }
    } else if (pageType.value === "followers") {
      response = await getFollowers(userId);
      if (response.data.code === 200) {
        const userIds = response.data.data.followers || [];
        // 获取用户详情
        const users = [];
        for (const id of userIds) {
          try {
            const userRes = await getUserInfo(id);
            if (userRes.data.code === 200) {
              users.push(userRes.data.data);
            }
          } catch (error) {
            console.error("加载用户信息失败:", error);
          }
        }
        items.value = users;
      }
    } else if (pageType.value === "favorites") {
      response = await getUserFavorites(userId);
      if (response.data.code === 200) {
        const postIds = response.data.data.favorites || [];
        // 这里可以加载帖子详情，暂时只显示 ID
        items.value = postIds.map(id => ({ id, title: `帖子 #${id}` }));
      }
    }
  } catch (error) {
    console.error("加载数据失败:", error);
  }
}

function handleItemClick(item) {
  if (pageType.value === "following" || pageType.value === "followers") {
    router.push(`/profile/${item.id}`);
  } else if (pageType.value === "favorites") {
    router.push(`/post/${item.id}`);
  }
}

function formatTime(time) {
  if (!time) return "";
  const date = new Date(time);
  return date.toLocaleString("zh-CN");
}
</script>

<style scoped>
.simple-page-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.page-header {
  font-size: 20px;
  font-weight: bold;
}

.page-content {
  height: calc(100% - 40px);
}

.item-card {
  cursor: pointer;
  transition: all 0.3s;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.item-content {
  padding: 10px;
}

.item-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.item-meta {
  color: #909399;
  font-size: 12px;
}

/* 让卡片占满可用宽度和高度 */
:deep(.el-card) {
  width: 100%;
  box-sizing: border-box;
}
</style>

