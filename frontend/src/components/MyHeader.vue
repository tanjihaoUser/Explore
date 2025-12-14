<template>
  <div class="header_main">
    <div style="font-size: 20px">
      <!-- 展开时显示向左收起图标，收起时显示向右展开图标 -->
      <el-icon v-if="expand" @click="changeStatus()"><DArrowLeft /></el-icon>
      <el-icon v-else @click="changeStatus()"><DArrowRight /></el-icon>
    </div>
    <h1>Redis社交平台</h1>
    <div style="font: 18px; display: flex; align-items: center; gap: 15px">
      <!-- 通知图标 -->
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-popover
          placement="bottom-end"
          :width="400"
          trigger="hover"
          popper-class="notification-popover"
        >
          <template #reference>
            <el-icon :size="24" style="cursor: pointer" @click="showNotificationDialog = true">
              <Bell />
            </el-icon>
          </template>
          <div class="notification-preview">
            <div v-if="previewNotifications.length === 0" class="empty-notifications">
              暂无通知
            </div>
            <div
              v-for="notification in previewNotifications"
              :key="notification.notificationId"
              class="notification-item"
              :class="{ unread: !notification.isRead }"
              @click="handleNotificationClick(notification)"
            >
              <div class="notification-content">{{ notification.content }}</div>
              <div class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</div>
            </div>
            <div class="notification-footer">
              <el-button type="primary" text @click="showNotificationDialog = true">查看全部</el-button>
            </div>
          </div>
        </el-popover>
      </el-badge>

      <el-dropdown style="font-size: 18px">
        <span class="el-dropdown-link">
          {{ username }}
          <el-icon class="el-icon--right">
            <arrow-down />
          </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item divided @click="router.push(`/profile/${userId}`)"
              >个人中心</el-dropdown-item
            >
            <el-dropdown-item divided @click="changePwdVisible = true"
              >修改密码</el-dropdown-item
            >
            <el-dropdown-item divided @click="router.push('/favorites')"
              >我的收藏</el-dropdown-item
            >
            <el-dropdown-item divided @click="router.push('/following')"
              >我的关注</el-dropdown-item
            >
            <el-dropdown-item divided @click="logout()"
              >退出登录</el-dropdown-item
            >
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 通知对话框 -->
    <el-dialog v-model="showNotificationDialog" title="通知中心" width="60%" @close="loadNotifications">
      <div class="notification-dialog-header">
        <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">
          全部标记为已读
        </el-button>
      </div>
      <div class="notification-list">
        <el-empty v-if="notifications.length === 0" description="暂无通知" />
        <div
          v-for="notification in notifications"
          :key="notification.notificationId"
          class="notification-item-full"
          :class="{ unread: !notification.isRead }"
        >
          <div class="notification-content-full">
            <div class="notification-text">{{ notification.content }}</div>
            <div class="notification-meta">
              <span class="notification-type">{{ getNotificationTypeText(notification.notificationType) }}</span>
              <span class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</span>
            </div>
          </div>
          <div class="notification-actions">
            <el-button
              v-if="!notification.isRead"
              type="primary"
              size="small"
              text
              @click="markAsRead(notification.notificationId)"
            >
              标记已读
            </el-button>
          </div>
        </div>
      </div>
      <el-pagination
        v-if="notificationTotal > 0"
        v-model:current-page="notificationPage"
        v-model:page-size="notificationPageSize"
        :total="notificationTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleNotificationSizeChange"
        @current-change="handleNotificationPageChange"
        style="margin-top: 20px; justify-content: center"
      />
    </el-dialog>

    <!-- 注意这里要用v-model，不能用v-show-->
    <el-dialog v-model="changePwdVisible" title="修改密码" center width="40%">
      <el-form :label-position="labelPosition" label-width="auto">
        <el-form-item label="原始密码：">
          <el-input type="password" v-model="newUserData.oldPwd" clearable />
        </el-form-item>
        <el-form-item label="新密码：">
          <el-input type="password" v-model="newUserData.newPwd" clearable />
        </el-form-item>
        <el-form-item label="确认密码：">
          <el-input type="password" v-model="verPwd" clearable />
        </el-form-item>
        <div style="display: flex; justify-content: center">
          <el-button type="success" @click="changePwd">确认</el-button>
          <el-button type="primary" @click="cancelChange(changePwdVisible)"
            >取消</el-button
          >
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { updatePassword, getUserInfo } from "@/api/user.js";
import { logout as logoutSession } from "@/api/session.js";
import { useMenuStore } from "@/stores/menu.js";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user.js";
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Bell } from "@element-plus/icons-vue";
import {
  getNotifications,
  getUnreadNotifications,
  getUnreadCount,
  markAsRead as markAsReadApi,
  markAllAsRead as markAllAsReadApi,
} from "@/api/notification.js";

const menuStore = useMenuStore();
const userStore = useUserStore();
const router = useRouter();
let { userId, username, sessionId } = storeToRefs(userStore);
let { expand } = storeToRefs(menuStore);
let changeNameVisible = ref(false);
let changePwdVisible = ref(false);
let newUserData = ref({
  username: username.value,
  oldPwd: "",
  newPwd: "",
});
let verPwd = ref("");

// 通知相关
let showNotificationDialog = ref(false);
let unreadCount = ref(0);
let notifications = ref([]);
let previewNotifications = ref([]);
let notificationPage = ref(1);
let notificationPageSize = ref(20);
let notificationTotal = ref(0);
let notificationPollingTimer = null;

onMounted(() => {
  loadUserInfo();
  if (userId.value) {
    loadUnreadCount();
    loadPreviewNotifications();
    // 每30秒轮询一次未读通知数量
    notificationPollingTimer = setInterval(() => {
      loadUnreadCount();
      loadPreviewNotifications();
    }, 30000);
  }
});

onUnmounted(() => {
  if (notificationPollingTimer) {
    clearInterval(notificationPollingTimer);
  }
});

async function loadUserInfo() {
  if (userId.value) {
    try {
      const response = await getUserInfo(userId.value);
      if (response.data.code === 200) {
        newUserData.value.username = response.data.data.username || username.value;
      }
    } catch (error) {
      console.error("加载用户信息失败:", error);
    }
  }
}

function changeStatus() {
  expand.value = !expand.value;
  menuStore.setExpand(expand.value);
}

function cancelChange() {
  if (changeNameVisible.value) changeNameVisible.value = false;
  else if (changePwdVisible.value) changePwdVisible.value = false;
  newUserData.value.oldPwd = "";
  newUserData.value.newPwd = "";
  verPwd.value = "";
}

async function changePwd() {
  if (newUserData.value.newPwd == "") {
    ElMessage.error("新密码不能为空");
  } else if (newUserData.value.newPwd != verPwd.value) {
    ElMessage.error("确认密码不一致");
  } else {
    try {
      const response = await updatePassword(userId.value, newUserData.value.oldPwd, newUserData.value.newPwd);
      if (response.data.code === 200) {
        ElMessage.success("密码修改成功，请重新登录");
        await handleLogout();
        changePwdVisible.value = false;
      }
    } catch (error) {
      newUserData.value.oldPwd = "";
      newUserData.value.newPwd = "";
      verPwd.value = "";
    }
  }
}

async function handleLogout() {
  try {
    if (sessionId.value) {
      await logoutSession(sessionId.value);
    }
  } catch (error) {
    console.error("登出失败:", error);
  } finally {
    userStore.clearUserInfo();
    router.push("/login");
  }
}

function logout() {
  ElMessageBox.confirm("确认要退出登录吗", "", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      handleLogout();
      ElMessage({
        type: "success",
        message: "退出成功",
      });
    })
    .catch(() => {});
}

// 通知相关函数
async function loadUnreadCount() {
  if (!userId.value) return;
  try {
    const response = await getUnreadCount(userId.value);
    if (response.data.code === 200) {
      unreadCount.value = response.data.data?.unreadCount || 0;
    }
  } catch (error) {
    console.error("加载未读通知数量失败:", error);
  }
}

async function loadPreviewNotifications() {
  if (!userId.value) return;
  try {
    const response = await getUnreadNotifications(userId.value, 5);
    if (response.data.code === 200) {
      previewNotifications.value = response.data.data?.unreadNotifications || [];
    }
  } catch (error) {
    console.error("加载预览通知失败:", error);
  }
}

async function loadNotifications() {
  if (!userId.value) return;
  try {
    const response = await getNotifications(userId.value, notificationPage.value, notificationPageSize.value);
    if (response.data.code === 200) {
      notifications.value = response.data.data?.notifications || [];
      notificationTotal.value = response.data.data?.total || 0;
    }
  } catch (error) {
    console.error("加载通知列表失败:", error);
  }
}

async function markAsRead(notificationId) {
  if (!userId.value) return;
  try {
    const response = await markAsReadApi(userId.value, notificationId);
    if (response.data.code === 200) {
      ElMessage.success("标记为已读成功");
      // 更新本地状态
      const notification = notifications.value.find((n) => n.notificationId === notificationId);
      if (notification) {
        notification.isRead = true;
      }
      const previewNotification = previewNotifications.value.find((n) => n.notificationId === notificationId);
      if (previewNotification) {
        previewNotification.isRead = true;
      }
      loadUnreadCount();
    }
  } catch (error) {
    console.error("标记已读失败:", error);
    ElMessage.error("标记已读失败");
  }
}

async function markAllAsRead() {
  if (!userId.value) return;
  try {
    const response = await markAllAsReadApi(userId.value);
    if (response.data.code === 200) {
      ElMessage.success("全部标记为已读成功");
      notifications.value.forEach((n) => (n.isRead = true));
      previewNotifications.value.forEach((n) => (n.isRead = true));
      unreadCount.value = 0;
    }
  } catch (error) {
    console.error("批量标记已读失败:", error);
    ElMessage.error("批量标记已读失败");
  }
}

function handleNotificationClick(notification) {
  showNotificationDialog.value = true;
  if (!notification.isRead) {
    markAsRead(notification.notificationId);
  }
}

function formatNotificationTime(time) {
  if (!time) return "";
  const date = new Date(time);
  const now = new Date();
  const diff = now - date;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit" });
}

function getNotificationTypeText(type) {
  const typeMap = {
    like: "点赞",
    favorite: "收藏",
    follow: "关注",
    comment: "评论",
  };
  return typeMap[type] || type;
}

function handleNotificationSizeChange(val) {
  notificationPageSize.value = val;
  loadNotifications();
}

function handleNotificationPageChange(val) {
  notificationPage.value = val;
  loadNotifications();
}

// 监听对话框打开，加载通知列表
watch(showNotificationDialog, (newVal) => {
  if (newVal) {
    loadNotifications();
  }
});
</script>

<style lang="scss" scoped>
.header_main {
  /* 三个div元素在同一行*/
  display: flex;
  justify-content: space-between;
  /* 上下居中 */
  align-items: center;
}
/* el-dropdown去除黑框*/
:deep(.el-tooltip__trigger:focus-visible) {
  outline: unset;
}
/* el-dialog中内容居中*/
:deep(.el-dialog__body) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-preview {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f5f7fa;
  }

  &.unread {
    background-color: #ecf5ff;
    font-weight: bold;
  }
}

.notification-content {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-footer {
  padding: 10px;
  text-align: center;
  border-top: 1px solid #ebeef5;
}

.notification-dialog-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.notification-list {
  max-height: 500px;
  overflow-y: auto;
}

.notification-item-full {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f5f7fa;
  }

  &.unread {
    background-color: #ecf5ff;
  }
}

.notification-content-full {
  flex: 1;
}

.notification-text {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.notification-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
}

.notification-type {
  padding: 2px 8px;
  background-color: #f0f2f5;
  border-radius: 4px;
}

.notification-actions {
  margin-left: 15px;
}
</style>
