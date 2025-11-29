<template>
  <div class="create-post-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEditMode ? '编辑帖子' : '发布新帖' }}</span>
        </div>
      </template>
      
      <el-form :model="postForm" label-width="80px" v-loading="loading">
        <el-form-item label="内容" required>
          <v-md-editor
            v-model="postForm.content"
            height="400px"
            placeholder="请输入帖子内容，支持 Markdown 格式。第一行以 # 开头的内容将作为标题"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEditMode ? '更新' : '发布' }}
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { createPost, updatePost, getPostById } from "@/api/post";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const postForm = ref({
  id: null,
  title: "",
  content: "",
  userId: userStore.userId,
});

const submitting = ref(false);
const loading = ref(false);

const isEditMode = computed(() => {
  return !!route.query.edit;
});

const editPostId = computed(() => {
  return route.query.edit ? Number(route.query.edit) : null;
});

onMounted(async () => {
  if (isEditMode.value && editPostId.value) {
    await loadPost(editPostId.value);
  }
});

async function loadPost(postId) {
  loading.value = true;
  try {
    const response = await getPostById(postId);
    const isSuccess = (response.data.code === 200) || (response.data.success === true);
    
    if (isSuccess) {
      const post = response.data.data;
      postForm.value = {
        id: post.id,
        title: extractTitle(post.content) || "",
        content: post.content || "",
        userId: post.userId,
      };
    } else {
      ElMessage.error("加载帖子失败");
      router.push("/home");
    }
  } catch (error) {
    console.error("加载帖子失败:", error);
    ElMessage.error("加载帖子失败");
    router.push("/home");
  } finally {
    loading.value = false;
  }
}

function extractTitle(content) {
  if (!content) return "";
  const lines = content.split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith('#')) {
      return trimmed.replace(/^#+\s*/, '');
    }
    if (trimmed) {
      return trimmed.substring(0, 100);
    }
  }
  return "";
}

async function handleSubmit() {
  if (!postForm.value.content.trim()) {
    ElMessage.warning("请输入内容");
    return;
  }
  
  if (!userStore.userId) {
    ElMessage.warning("请先登录");
    router.push("/login");
    return;
  }
  
  submitting.value = true;
  try {
    if (isEditMode.value) {
      // 编辑模式：更新帖子
      const data = {
        id: postForm.value.id,
        content: postForm.value.content,
        userId: userStore.userId,
      };
      
      const response = await updatePost(data);
      const isSuccess = (response.data.code === 200) || (response.data.success === true);
      
      if (isSuccess) {
        ElMessage.success("更新成功");
        router.push("/home");
      } else {
        ElMessage.error(response.data.message || "更新失败");
      }
    } else {
      // 新建模式：创建帖子
      const data = {
        content: postForm.value.content,
        userId: userStore.userId,
      };
      
      const response = await createPost(data);
      const isSuccess = (response.data.code === 200) || (response.data.success === true);
      
      if (isSuccess) {
        ElMessage.success("发布成功");
        router.push("/home");
      } else {
        ElMessage.error(response.data.message || "发布失败");
      }
    }
  } catch (error) {
    console.error("操作失败:", error);
    ElMessage.error(isEditMode.value ? "更新失败" : "发布失败");
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  router.back();
}
</script>

<style scoped>
.create-post-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
}

/* 让卡片铺满整个 el-main 高度和宽度 */
:deep(.el-card) {
  height: 100%;
  width: 100%;
  box-sizing: border-box;
}
</style>

