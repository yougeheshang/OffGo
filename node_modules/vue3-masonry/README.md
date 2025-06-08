# Vue3-Masonry
> Vue3 Masonry component

## Installation
```bash
npm install vue3-masonry
```

## Usage
```vue
<Masonry
:items="itemList"
:column-width="columnWidth"
:columns-num="columnNum"
:gap="10"
>
   <template #default="{item}">
      <img :src="item.url" :width="columnWidth"/>
   </template>
</Masonry>
```

## Props
| Name | Type | Default | Description |
| --- | --- | --- | --- |
| items | Array | [] | List of items to display |
| column-width | Number | 200 | Width of each column |
| columns-num | Number | 3 | Number of columns |
| gap | Number | 10 | Gap between columns |

