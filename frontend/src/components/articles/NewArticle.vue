<template>
  <el-form> </el-form>
  <div style="display: flex; align-items: center">
    文章标题：<el-input v-model="data.title" style="width: 600px"></el-input>
  </div>
  <div style="margin-top: 2%">
    <div>具体内容（使用Markdown语法）</div>
    <el-scrollbar style="height: 100%">
      <div style="margin-top: 1%">
        <v-md-editor v-model="data.content" height="400px"></v-md-editor>
      </div>
    </el-scrollbar>
  </div>
  <div style="justify-content: center; display: flex; margin-top: 1%">
    <el-button type="primary" @click="submit()">确认</el-button>
    <el-button type="info" @click="router.back()">取消</el-button>
  </div>
</template>

<script setup>
import { useUserStore } from "@/stores/user";
import { storeToRefs } from "pinia";
import { insert, changeDetail, getUpdateDetail } from "@/api/article";
import { ElMessage } from "element-plus";
import { onMounted } from "vue";
import { useRoute } from "vue-router";

let { userId } = storeToRefs(useUserStore());
const router = useRouter();
// 从url中获取到对应的博客Id
let articleId = useRoute().params.articleId;

let data = ref({
  // 新增博客时使用
  userId: userId.value,
  // 修改博客时传入博客id
  articleId: articleId,
  title: "",
  content: "",
});

// 如果博客ID不是0，说明是修改，需要查询数据填充
onMounted(() => {
  if (articleId != 0) {
    getData();
  }
});

function getData() {
  getUpdateDetail(articleId).then((response) => {
    let res = response.data;
    if (res.code == 200) {
      data.value = res.data;
      data.value.articleId = articleId;
    }
  });
}

function submit() {
  // 通过判断博客id是否为0判断新增和修改
  if (articleId == 0) {
    insert(data.value).then((response) => {
      let res = response.data;
      if (res.code == 200) {
        ElMessage.success("添加成功");
      }
    });
  } else {
    changeDetail(data.value).then((response) => {
      let res = response.data;
      if (res.code == 200) {
        ElMessage.success("修改成功");
      }
    });
  }
  // 添加成功之后返回上一级，go(-1)：原页面表单中数据会丢失，效果：后退 + 刷新
  router.go(-1);
  // 清理数据，便于下次输入
  data.value.title = "";
  data.value.content = "";
  data.value.articleId = "";
}
</script>

<style scoped></style>
