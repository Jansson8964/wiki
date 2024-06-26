<template>
  <a-layout>
    <a-layout-content
        :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
    >
      <p>
        <a-form
            layout="inline"
            :model="categoryQueryForm"
            @finish="handleQueryFormSubmit"
        >

          <a-form-item>
            <a-button
                type="primary"
                html-type="submit"
                size="large"
            >
              query
            </a-button>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="add()" size="large">add</a-button>
          </a-form-item>
        </a-form>
      </p>
      <a-table
          :columns="columns"
          :row-key="record => record.id"
          :data-source="level1"
          :pagination="false"
          :loading="loading"
      >
        <template #cover="{ text: cover }">
          <img v-if="cover" :src="cover" alt="avatar" />
        </template>
        <template v-slot:action="{ text, record }">
          <a-space size="small">
            <a-button type="primary" @click="edit(record)">
              edit
            </a-button>
            <a-popconfirm
                title="confirm delete?"
                ok-text="Yes"
                cancel-text="No"
                @confirm="handleDeleteCategory(record.id)"
            >
              <a-button type="danger">delete</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-layout-content>
  </a-layout>

  <a-modal
      title="category form"
      v-model:visible="modalVisible"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
  >
    <a-form :model="category" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="name">
        <a-input v-model:value="category.name" />
      </a-form-item>
      <a-form-item label="parent category">
        <a-select
            v-model:value="category.parent"
            style="width: 120px"
            @focus="focus"
            ref="select"
        >
          <a-select-option value="0">
            none
          </a-select-option>
          <a-select-option v-for="c in level1"
                           :key="c.id"
                           :value="c.id"
                           :disabled="category.id === c.id">
            {{ c.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="sort">
        <a-input v-model:value="category.sort" type="textarea" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts">
import { defineComponent, onMounted, ref, UnwrapRef, reactive } from 'vue';
import axios from 'axios';
import { message } from 'ant-design-vue'
import { Tool } from "@/util/tool";
import { Category, CategoryQueryForm } from "@/models";


export default defineComponent({
  name: 'AdminCategory',
  setup() {
    const categoryQueryForm: UnwrapRef<CategoryQueryForm> = reactive({
      name: ''
    });

    const categorys = ref<Category[]>([]);
    const loading = ref(false);

    const columns = [
      {
        title: 'name',
        dataIndex: 'name'
      },
      {
        title: 'parent category',
        key: 'parent',
        dataIndex: 'parent'
      },
      {
        title: 'sort',
        dataIndex: 'sort'
      },
      {
        title: 'Action',
        key: 'action',
        slots: { customRender: 'action' }
      }
    ];

    /**
     * 一级分类树，children属性就是二级分类
     * [{
     *   id: "",
     *   name: "",
     *   children: [{
     *     id: "",
     *     name: "",
     *   }]
     * }]
     */
    const level1 = ref(); // 一级分类树，children属性就是二级分类


    /**
     * 数据query
     **/
    const handleQuery = () => {
      loading.value = true;
      axios.get("/category/all").then((response) => {
        loading.value = false;
        const respData = response.data;

        if (respData.code == 0) {
          categorys.value = respData.data;
          console.log("native array：", categorys.value);

          level1.value = [];
          level1.value = Tool.array2Tree(categorys.value, 0);
          console.log("tree structure：", level1);

        } else {
          message.error(respData.msg);
        }
      });
    };

    /**
     * 根据表单提交的数据进行query
     **/
    const handleQueryFormSubmit = () => {
      handleQuery();
    };


    // -------- 表单 ---------
    const category = ref();
    const modalVisible = ref(false);
    const modalLoading = ref(false);
    const handleModalOk = () => {
      modalLoading.value = true;
      axios.post("/category/save", category.value).then((response) => {
        const respData = response.data;
        modalLoading.value = false;
        if (respData.code == 0) {
          modalVisible.value = false;
        } else {
          message.error(respData.msg);
        }
        handleQuery();
      })
    };

    /**
     * edit
     */
    const edit = (record: any) => {
      modalVisible.value = true;
      category.value = Tool.copy(record);
    };

    /**
     * add
     */
    const add = () => {
      modalVisible.value = true;
      category.value = {};
    }

    /**
     * delete
     */
    const handleDeleteCategory = (categoryId: string) => {
      console.log(categoryId);
      axios.delete("/category/delete/" + categoryId).then((response) => {
        const respData = response.data;
        if (respData.code == 0) {
          handleQuery();
        }
      });
    }


    onMounted(() => {
      handleQuery();
    });

    return {

      categoryQueryForm,
      level1,
      labelCol: { span: 4 },
      wrapperCol: { span: 14 },

      columns,
      loading,

      edit,
      add,
      handleDeleteCategory,
      handleQueryFormSubmit,

      category,
      modalVisible,
      modalLoading,
      handleModalOk
    }
  }
});
</script>

<style scoped>
img {
  width: 50px;
  height: 50px;
}
</style>
