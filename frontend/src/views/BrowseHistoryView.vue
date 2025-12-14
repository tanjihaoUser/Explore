<template>
  <div class="browse-history-container">
    <el-card class="header-card">
      <template #header>
        <div class="header-content">
          <h2>浏览记录</h2>
          <div class="header-actions">
            <el-button type="danger" @click="handleClearAll" :loading="clearing">
              <el-icon><Delete /></el-icon>
              清空记录
            </el-button>
            <el-button type="info" @click="handleRefresh" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="stats-info">
        <el-statistic title="总浏览数" :value="totalCount" />
        <el-statistic title="当前页" :value="`${currentPage} / ${totalPages}`" />
      </div>
    </el-card>

    <el-card class="posts-card">
      <el-empty v-if="posts.length === 0 && !loading" description="暂无浏览记录" />
      <el-scrollbar v-else style="height: calc(100vh - 300px)">
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
                <div class="post-time">
                  <span>浏览时间：{{ formatTime(post.browseTime) }}</span>
                </div>
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
            </div>
          </el-card>
        </div>
      </el-scrollbar>
    </el-card>

    <el-pagination
      v-if="totalPages > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="totalCount"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
      class="pagination"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowUp,
  Star,
  ChatDotRound,
  View,
  Delete,
  Refresh,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import {
  getBrowseHistoryPage,
  clearBrowseHistory,
  getBrowseHistoryCount,
} from "@/api/browseHistory";
import { getPostById } from "@/api/post";
import { likePost, unlikePost, checkLiked } from "@/api/relation";
import { favoritePost, unfavoritePost, checkFavorited } from "@/api/relation";

const router = useRouter();
const userStore = useUserStore();

const posts = ref([]);
const loading = ref(false);
const clearing = ref(false);
const currentPage = ref(1);
const pageSize = ref(20);
const totalCount = ref(0);

const totalPages = computed(() => {
  return Math.ceil(totalCount.value / pageSize.value);
});

onMounted(() => {
  loadBrowseHistory();
  loadTotalCount();
});

async function loadBrowseHistory() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  loading.value = true;
  try {
    const response = await getBrowseHistoryPage(
      userStore.userId,
      currentPage.value,
      pageSize.value
    );

    const isSuccess =
      response.data.code === 200 || response.data.success === true;
    if (isSuccess) {
      const data = response.data.data || response.data;
      const postIds = data.postIds || [];
      totalCount.value = data.total || 0;

      // 加载帖子详情
      const postPromises = postIds.map((postId) => getPostById(postId));
      const postResponses = await Promise.allSettled(postPromises);

      const loadedPosts = [];
      for (let i = 0; i < postResponses.length; i++) {
        const result = postResponses[i];
        if (result.status === "fulfilled") {
          const postRes = result.value;
          const postSuccess =
            postRes.data.code === 200 || postRes.data.success === true;
          if (postSuccess) {
            const post = postRes.data.data || postRes.data;
            // 获取浏览时间
            try {
              const { getBrowseTime } = await import("@/api/browseHistory");
              const timeRes = await getBrowseTime(
                userStore.userId,
                postIds[i]
              );
              const timeSuccess =
                timeRes.data.code === 200 || timeRes.data.success === true;
              if (timeSuccess) {
                post.browseTime =
                  timeRes.data.data?.browseTime ||
                  timeRes.data.browseTime ||
                  null;
              }
            } catch (error) {
              console.error("获取浏览时间失败:", error);
            }
            loadedPosts.push(post);
          }
        }
      }

      // 检查每个帖子的点赞和收藏状态
      for (const post of loadedPosts) {
        try {
          const [likedRes, favoritedRes] = await Promise.all([
            checkLiked(userStore.userId, post.id),
            checkFavorited(userStore.userId, post.id),
          ]);

          const getData = (res) => {
            const success =
              res.data.code === 200 || res.data.success === true;
            return success ? res.data.data || res.data : {};
          };

          post.isLiked = getData(likedRes).isLiked || false;
          post.isFavorited = getData(favoritedRes).isFavorited || false;
        } catch (error) {
          console.error("检查帖子状态失败:", error);
        }
      }

      posts.value = loadedPosts;
    } else {
      ElMessage.error(response.data.message || "加载浏览记录失败");
    }
  } catch (error) {
    console.error("加载浏览记录失败:", error);
    ElMessage.error("加载浏览记录失败");
  } finally {
    loading.value = false;
  }
}

async function loadTotalCount() {
  if (!userStore.userId) return;

  try {
    const response = await getBrowseHistoryCount(userStore.userId);
    const isSuccess =
      response.data.code === 200 || response.data.success === true;
    if (isSuccess) {
      totalCount.value = response.data.data?.count || response.data.count || 0;
    }
  } catch (error) {
    console.error("获取浏览记录总数失败:", error);
  }
}

function handlePageChange(page) {
  currentPage.value = page;
  loadBrowseHistory();
}

function handleSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
  loadBrowseHistory();
}

function handleRefresh() {
  loadBrowseHistory();
  loadTotalCount();
}

async function handleClearAll() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  try {
    await ElMessageBox.confirm("确定要清空所有浏览记录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });

    clearing.value = true;
    const response = await clearBrowseHistory(userStore.userId);
    const isSuccess =
      response.data.code === 200 || response.data.success === true;
    if (isSuccess) {
      ElMessage.success("浏览记录已清空");
      posts.value = [];
      totalCount.value = 0;
      currentPage.value = 1;
    } else {
      ElMessage.error(response.data.message || "清空失败");
    }
  } catch (error) {
    if (error !== "cancel") {
      console.error("清空浏览记录失败:", error);
      ElMessage.error("清空浏览记录失败");
    }
  } finally {
    clearing.value = false;
  }
}

function goToDetail(postId) {
  router.push(`/post/${postId}`);
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
    ElMessage.error("操作失败");
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
    ElMessage.error("操作失败");
  }
}

function extractTitle(content) {
  if (!content) return "无标题";
  const lines = content.split("\n");
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith("#")) {
      return trimmed.length > 50 ? trimmed.substring(0, 50) + "..." : trimmed;
    }
  }
  return "无标题";
}

function formatContent(content) {
  if (!content) return "";
  // 简单的格式化，实际可以使用 markdown 渲染
  return content
    .replace(/\n/g, "<br>")
    .substring(0, 200) + (content.length > 200 ? "..." : "");
}

function formatTime(timestamp) {
  if (!timestamp) return "未知时间";
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) {
    return `${days}天前`;
  } else if (hours > 0) {
    return `${hours}小时前`;
  } else if (minutes > 0) {
    return `${minutes}分钟前`;
  } else {
    return "刚刚";
  }
}
</script>

<style scoped lang="scss">
.browse-history-container {
  padding: 20px;
  height: 100%;

  .header-card {
    margin-bottom: 20px;

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h2 {
        margin: 0;
        font-size: 24px;
      }

      .header-actions {
        display: flex;
        gap: 10px;
      }
    }

    .stats-info {
      display: flex;
      gap: 40px;
      margin-top: 20px;
    }
  }

  .posts-card {
    .posts-wrapper {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .post-card {
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .post-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .post-author {
          display: flex;
          align-items: center;
          gap: 10px;

          .author-name {
            font-weight: 500;
          }
        }

        .post-time {
          font-size: 12px;
          color: #909399;
        }
      }

      .post-content {
        .post-title {
          margin: 10px 0;
          font-size: 18px;
          font-weight: 600;
        }

        .post-body {
          color: #606266;
          line-height: 1.6;
        }
      }

      .post-footer {
        margin-top: 15px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .action-buttons {
          display: flex;
          gap: 10px;
          align-items: center;

          .badge-item {
            :deep(.el-badge__content) {
              top: -5px;
              right: -5px;
            }
          }
        }
      }
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>

