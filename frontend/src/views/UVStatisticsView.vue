<template>
  <div class="uv-statistics-container">
    <!-- 统计概览卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总帖子数</div>
            <div class="stat-value">{{ postsList.length }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总UV</div>
            <div class="stat-value">{{ totalUV }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总点赞数</div>
            <div class="stat-value">{{ totalLikes }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总收藏数</div>
            <div class="stat-value">{{ totalFavorites }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总评论数</div>
            <div class="stat-value">{{ totalComments }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-label">平均UV</div>
            <div class="stat-value">{{ averageUV.toFixed(1) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧：帖子列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的帖子</span>
              <el-button size="small" @click="loadUserPosts">刷新</el-button>
            </div>
          </template>
          <div v-if="loadingPosts" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>
          <div v-else-if="postsList.length > 0" class="posts-list">
            <div
              v-for="post in postsList"
              :key="post.postId"
              class="post-item"
              :class="{ active: selectedPostId === post.postId }"
              @click="selectPost(post.postId)"
            >
              <div class="post-content">{{ post.content }}</div>
              <div class="post-meta">
                <div class="post-stats">
                  <span class="uv-badge">UV: {{ post.totalUV || 0 }}</span>
                  <span class="like-badge">👍 {{ post.likeCount || 0 }}</span>
                  <span class="favorite-badge">⭐ {{ post.favoriteCount || 0 }}</span>
                  <span class="comment-badge">💬 {{ post.commentCount || 0 }}</span>
                </div>
                <span class="post-date">{{ formatDate(post.createdAt) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无帖子" />
        </el-card>
      </el-col>

      <!-- 右侧：UV曲线图 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>UV趋势图</span>
              <div class="header-controls">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYYMMDD"
                  @change="loadDailyUV"
                  style="width: 300px; margin-right: 10px"
                />
                <el-button type="primary" @click="loadDailyUV">查询</el-button>
              </div>
            </div>
          </template>

          <div v-if="loadingChart" class="loading-container">
            <el-skeleton :rows="10" animated />
          </div>

          <div v-else-if="selectedPostId && chartData.length > 0" class="chart-container">
            <!-- 统计指标选择 -->
            <div style="margin-bottom: 20px">
              <el-radio-group v-model="selectedMetric" @change="renderChart">
                <el-radio-button label="uv">UV</el-radio-button>
                <el-radio-button label="likes">点赞</el-radio-button>
                <el-radio-button label="favorites">收藏</el-radio-button>
                <el-radio-button label="comments">评论</el-radio-button>
                <el-radio-button label="all">全部</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="chartRef" style="width: 100%; height: 400px"></div>
          </div>

          <el-empty v-else description="请选择一个帖子查看统计数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { getPostStatisticsInRange, getUserPostsUV } from "@/api/uvStatistics";
import { useUserStore } from "@/stores/user";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

const userStore = useUserStore();

// 数据
const postsList = ref([]);
const selectedPostId = ref(null);
const dateRange = ref([]);
const dailyUVData = ref({});
const postStatisticsData = ref({});
const selectedMetric = ref("all");
const loadingPosts = ref(false);
const loadingChart = ref(false);

// 图表
const chartRef = ref(null);
let chartInstance = null;

// 计算属性
const totalUV = computed(() => {
  return postsList.value.reduce((sum, post) => sum + (post.totalUV || 0), 0);
});

const totalLikes = computed(() => {
  return postsList.value.reduce((sum, post) => sum + (post.likeCount || 0), 0);
});

const totalFavorites = computed(() => {
  return postsList.value.reduce((sum, post) => sum + (post.favoriteCount || 0), 0);
});

const totalComments = computed(() => {
  return postsList.value.reduce((sum, post) => sum + (post.commentCount || 0), 0);
});

const averageUV = computed(() => {
  if (postsList.value.length === 0) return 0;
  return totalUV.value / postsList.value.length;
});

const maxUV = computed(() => {
  if (postsList.value.length === 0) return 0;
  return Math.max(...postsList.value.map((post) => post.totalUV || 0));
});

const chartData = computed(() => {
  if (!selectedPostId.value) {
    return [];
  }
  const stats = postStatisticsData.value[selectedPostId.value];
  if (!stats) {
    return [];
  }
  
  const dailyUV = stats.dailyUV || {};
  const dailyLikes = stats.dailyLikes || {};
  const dailyFavorites = stats.dailyFavorites || {};
  const dailyComments = stats.dailyComments || {};
  
  // 获取所有日期
  const allDates = new Set([
    ...Object.keys(dailyUV),
    ...Object.keys(dailyLikes),
    ...Object.keys(dailyFavorites),
    ...Object.keys(dailyComments),
  ]);
  
  return Array.from(allDates).sort().map((date) => ({
    date,
    uv: dailyUV[date] || 0,
    likes: dailyLikes[date] || 0,
    favorites: dailyFavorites[date] || 0,
    comments: dailyComments[date] || 0,
  }));
});

// 生命周期
onMounted(() => {
  loadUserPosts();
  // 默认选择最近7天
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 7);
  dateRange.value = [
    formatDateForPicker(start),
    formatDateForPicker(end),
  ];
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
});

// 监听选中帖子变化
watch(selectedPostId, (newPostId) => {
  if (newPostId) {
    loadDailyUV();
  }
});

// 方法
async function loadUserPosts() {
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    return;
  }

  loadingPosts.value = true;
  try {
    const response = await getUserPostsUV(userStore.userId);
    if (response.data.success) {
      postsList.value = response.data.data.posts || [];
      // 自动选择第一个帖子
      if (postsList.value.length > 0 && !selectedPostId.value) {
        selectedPostId.value = postsList.value[0].postId;
      }
    } else {
      ElMessage.error(response.data.message || "加载帖子列表失败");
    }
  } catch (error) {
    console.error("加载帖子列表失败:", error);
    ElMessage.error("加载帖子列表失败");
  } finally {
    loadingPosts.value = false;
  }
}

async function loadDailyUV() {
  if (!selectedPostId.value) {
    return;
  }

  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning("请选择日期范围");
    return;
  }

  loadingChart.value = true;
  try {
    const [startDate, endDate] = dateRange.value;
    const response = await getPostStatisticsInRange(selectedPostId.value, startDate, endDate);
    
    if (response.data.success) {
      const data = response.data.data;
      postStatisticsData.value[selectedPostId.value] = data;
      
      await nextTick();
      renderChart();
    } else {
      ElMessage.error(response.data.message || "加载统计数据失败");
    }
  } catch (error) {
    console.error("加载统计数据失败:", error);
    ElMessage.error("加载统计数据失败");
  } finally {
    loadingChart.value = false;
  }
}

function selectPost(postId) {
  selectedPostId.value = postId;
}

function renderChart() {
  if (!chartRef.value || chartData.value.length === 0) {
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const dates = chartData.value.map((item) => formatDateDisplay(item.date));
  const uvs = chartData.value.map((item) => item.uv);
  const likes = chartData.value.map((item) => item.likes);
  const favorites = chartData.value.map((item) => item.favorites);
  const comments = chartData.value.map((item) => item.comments);

  // 根据选择的指标决定显示哪些数据
  const series = [];
  const legendData = [];

  if (selectedMetric.value === "all" || selectedMetric.value === "uv") {
    series.push({
      name: "UV",
      type: "line",
      data: uvs,
      smooth: true,
      areaStyle: {
        opacity: 0.3,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(64, 158, 255, 0.5)" },
          { offset: 1, color: "rgba(64, 158, 255, 0.1)" },
        ]),
      },
      lineStyle: { width: 2, color: "#409EFF" },
      itemStyle: { color: "#409EFF" },
    });
    legendData.push("UV");
  }

  if (selectedMetric.value === "all" || selectedMetric.value === "likes") {
    series.push({
      name: "点赞",
      type: "line",
      data: likes,
      smooth: true,
      lineStyle: { width: 2, color: "#F56C6C" },
      itemStyle: { color: "#F56C6C" },
    });
    legendData.push("点赞");
  }

  if (selectedMetric.value === "all" || selectedMetric.value === "favorites") {
    series.push({
      name: "收藏",
      type: "line",
      data: favorites,
      smooth: true,
      lineStyle: { width: 2, color: "#E6A23C" },
      itemStyle: { color: "#E6A23C" },
    });
    legendData.push("收藏");
  }

  if (selectedMetric.value === "all" || selectedMetric.value === "comments") {
    series.push({
      name: "评论",
      type: "line",
      data: comments,
      smooth: true,
      lineStyle: { width: 2, color: "#67C23A" },
      itemStyle: { color: "#67C23A" },
    });
    legendData.push("评论");
  }

  const option = {
    title: {
      text: "统计数据趋势",
      left: "center",
      textStyle: {
        fontSize: 16,
      },
    },
    tooltip: {
      trigger: "axis",
      formatter: function (params) {
        let result = chartData.value[params[0].dataIndex].date + "<br/>";
        params.forEach((param) => {
          result += `${param.seriesName}: ${param.value}<br/>`;
        });
        return result;
      },
    },
    legend: {
      data: legendData,
      top: 30,
    },
    xAxis: {
      type: "category",
      data: dates,
      boundaryGap: false,
      axisLabel: {
        rotate: 45,
      },
    },
    yAxis: {
      type: "value",
      name: "数量",
    },
    series: series,
    grid: {
      left: "3%",
      right: "4%",
      bottom: "15%",
      top: "20%",
      containLabel: true,
    },
  };

  chartInstance.setOption(option);

  // 响应式调整
  window.addEventListener("resize", () => {
    chartInstance?.resize();
  });
}

function formatDate(dateStr) {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return date.toLocaleDateString("zh-CN");
}

function formatDateDisplay(dateStr) {
  // dateStr 格式：yyyyMMdd
  if (!dateStr || dateStr.length !== 8) return dateStr;
  const year = dateStr.substring(0, 4);
  const month = dateStr.substring(4, 6);
  const day = dateStr.substring(6, 8);
  return `${month}-${day}`;
}

function formatDateForPicker(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}${month}${day}`;
}
</script>

<style scoped>
.uv-statistics-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-controls {
  display: flex;
  align-items: center;
}

.posts-list {
  max-height: 600px;
  overflow-y: auto;
}

.post-item {
  padding: 15px;
  margin-bottom: 10px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.post-item:hover {
  border-color: #409EFF;
  background-color: #F5F7FA;
}

.post-item.active {
  border-color: #409EFF;
  background-color: #ECF5FF;
}

.post-content {
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.post-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.uv-badge {
  background-color: #409EFF;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.like-badge {
  background-color: #F56C6C;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.favorite-badge {
  background-color: #E6A23C;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.comment-badge {
  background-color: #67C23A;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.post-date {
  color: #C0C4CC;
}

.chart-container {
  margin-top: 20px;
}

.loading-container {
  padding: 20px;
}
</style>

