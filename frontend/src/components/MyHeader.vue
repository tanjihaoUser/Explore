<template>
  <div class="header_main">
    <div style="font-size: 20px">
      <!-- 展开时显示向左收起图标，收起时显示向右展开图标 -->
      <el-icon v-if="expand" @click="changeStatus()"><DArrowLeft /></el-icon>
      <el-icon v-else @click="changeStatus()"><DArrowRight /></el-icon>
    </div>
    <h1>Redis社交平台</h1>
    <div style="font: 18px">
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
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";

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

onMounted(() => {
  loadUserInfo();
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
</style>
