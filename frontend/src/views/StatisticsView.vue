<template>
  <div class="statistics-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>流量走势图</h2>
          <div class="header-controls">
            <el-select v-model="selectedMetric" @change="loadStatistics" style="width: 200px; margin-right: 10px">
              <el-option label="主页访问量" value="homepage" />
              <el-option label="帖子浏览量" value="post" />
              <el-option label="点赞变化" value="like" />
              <el-option label="收藏变化" value="favorite" />
              <el-option label="综合统计" value="comprehensive" />
            </el-select>
            <el-select v-model="selectedHours" @change="loadStatistics" style="width: 150px; margin-right: 10px">
              <el-option label="最近6小时" :value="6" />
              <el-option label="最近12小时" :value="12" />
              <el-option label="最近24小时" :value="24" />
              <el-option label="最近48小时" :value="48" />
              <el-option label="最近7天" :value="168" />
            </el-select>
            <el-input
              v-if="selectedMetric === 'post'"
              v-model.number="postId"
              placeholder="帖子ID（可选）"
              style="width: 150px; margin-right: 10px"
              @keyup.enter="loadStatistics"
            />
            <el-button type="primary" @click="loadStatistics">刷新</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-else-if="chartData.length > 0" class="chart-container">
        <div ref="chartRef" style="width: 100%; height: 500px"></div>
      </div>

      <el-empty v-else description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import {
  getPostViewStatistics,
  getHomePageViewStatistics,
  getLikeStatistics,
  getFavoriteStatistics,
  getComprehensiveStatistics,
} from "@/api/statistics";

const selectedMetric = ref("homepage");
const selectedHours = ref(24);
const postId = ref(null);
const loading = ref(false);
const chartData = ref([]);
const chartRef = ref(null);
let chartInstance = null;

onMounted(() => {
  loadStatistics();
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
});

async function loadStatistics() {
  loading.value = true;
  try {
    let response;
    
    switch (selectedMetric.value) {
      case "homepage":
        response = await getHomePageViewStatistics(selectedHours.value);
        break;
      case "post":
        if (postId.value) {
          response = await getPostViewStatistics(postId.value, selectedHours.value);
        } else {
          ElMessage.warning("请输入帖子ID");
          loading.value = false;
          return;
        }
        break;
      case "like":
        response = await getLikeStatistics(postId.value, selectedHours.value);
        break;
      case "favorite":
        response = await getFavoriteStatistics(postId.value, selectedHours.value);
        break;
      case "comprehensive":
        response = await getComprehensiveStatistics(postId.value, selectedHours.value);
        break;
    }

    if (response.data.code === 200) {
      const data = response.data.data;
      
      if (selectedMetric.value === "comprehensive") {
        // 综合统计需要特殊处理
        chartData.value = data.statistics;
        renderComprehensiveChart(data.statistics);
      } else {
        chartData.value = data.data || [];
        renderChart(data.data || [], data);
      }
    } else {
      ElMessage.error(response.data.message || "加载数据失败");
    }
  } catch (error) {
    console.error("加载统计数据失败:", error);
    ElMessage.error("加载统计数据失败");
  } finally {
    loading.value = false;
  }
}

function renderChart(data, meta) {
  if (!chartRef.value) return;
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const times = data.map((item) => {
    const date = new Date(item.time);
    return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  });
  const values = data.map((item) => item.value);

  const option = {
    title: {
      text: getChartTitle(),
      left: "center",
    },
    tooltip: {
      trigger: "axis",
      formatter: function (params) {
        const param = params[0];
        const dataIndex = param.dataIndex;
        const originalData = data[dataIndex];
        const date = new Date(originalData.time);
        return `${date.toLocaleString("zh-CN")}<br/>${param.seriesName}: ${param.value}`;
      },
    },
    xAxis: {
      type: "category",
      data: times,
      boundaryGap: false,
    },
    yAxis: {
      type: "value",
    },
    series: [
      {
        name: getSeriesName(),
        type: "line",
        data: values,
        smooth: true,
        areaStyle: {
          opacity: 0.3,
        },
        lineStyle: {
          width: 2,
        },
      },
    ],
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true,
    },
  };

  chartInstance.setOption(option);
  
  // 响应式调整
  window.addEventListener("resize", () => {
    chartInstance?.resize();
  });
}

function renderComprehensiveChart(statistics) {
  if (!chartRef.value) return;
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const viewData = statistics.viewStatistics || [];
  const likeData = statistics.likeStatistics || [];
  const favoriteData = statistics.favoriteStatistics || [];

  const times = viewData.map((item) => {
    const date = new Date(item.time);
    return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit" });
  });

  const option = {
    title: {
      text: "综合数据统计",
      left: "center",
    },
    tooltip: {
      trigger: "axis",
    },
    legend: {
      data: ["浏览量", "点赞数", "收藏数"],
      top: 30,
    },
    xAxis: {
      type: "category",
      data: times,
      boundaryGap: false,
    },
    yAxis: {
      type: "value",
    },
    series: [
      {
        name: "浏览量",
        type: "line",
        data: viewData.map((item) => item.value),
        smooth: true,
      },
      {
        name: "点赞数",
        type: "line",
        data: likeData.map((item) => item.value),
        smooth: true,
      },
      {
        name: "收藏数",
        type: "line",
        data: favoriteData.map((item) => item.value),
        smooth: true,
      },
    ],
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true,
    },
  };

  chartInstance.setOption(option);
  
  window.addEventListener("resize", () => {
    chartInstance?.resize();
  });
}

function getChartTitle() {
  const titles = {
    homepage: "主页访问量统计",
    post: "帖子浏览量统计",
    like: "点赞变化曲线",
    favorite: "收藏变化曲线",
    comprehensive: "综合数据统计",
  };
  return titles[selectedMetric.value] || "流量走势图";
}

function getSeriesName() {
  const names = {
    homepage: "访问量",
    post: "浏览量",
    like: "点赞数",
    favorite: "收藏数",
  };
  return names[selectedMetric.value] || "数值";
}
</script>

<style scoped>
.statistics-container {
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

.header-controls {
  display: flex;
  align-items: center;
}

.chart-container {
  margin-top: 20px;
}

.loading-container {
  padding: 20px;
}
</style>

