package com.song.wiki.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.song.wiki.domain.Content;
import com.song.wiki.domain.Doc;
import com.song.wiki.domain.DocExample;
import com.song.wiki.dto.req.DocQueryReqDto;
import com.song.wiki.dto.req.DocSaveReqDto;
import com.song.wiki.dto.resp.DocQueryRespDto;
import com.song.wiki.dto.resp.PageRespDto;
import com.song.wiki.utils.CopyUtil;
import com.song.wiki.utils.RedisUtil;
import com.song.wiki.utils.RequestContext;
import com.song.wiki.utils.SnowFlake;
import com.song.wiki.exception.BusinessException;
import com.song.wiki.exception.BusinessExceptionCode;
import com.song.wiki.mapper.ContentMapper;
import com.song.wiki.mapper.DocMapper;
import com.song.wiki.mapper.DocMapperCustom;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DocService {

    private static final Logger LOG = LoggerFactory.getLogger(DocService.class);

    @Resource
    private DocMapper docMapper;

    @Resource
    private ContentMapper contentMapper;

    @Resource
    private DocMapperCustom docMapperCustom;

    @Resource
    private SnowFlake snowFlake;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private WsService wsService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 获取全部 Doc
     */
    public List<DocQueryRespDto> queryDocs(Long ebookId) {
        DocExample docExample = new DocExample();
        docExample.createCriteria().andEbookIdEqualTo(ebookId);
        docExample.setOrderByClause("sort asc");
        List<Doc> docList = docMapper.selectByExample(docExample);
        return CopyUtil.copyList(docList, DocQueryRespDto.class);
    }


    /**
     * 根据query条件对数据库中的 doc 进行query并返回query到的 doc
     */
    public PageRespDto<DocQueryRespDto> queryDocs(DocQueryReqDto reqDto) {
        DocExample docExample = new DocExample();
        DocExample.Criteria criteria = docExample.createCriteria();
        PageHelper.startPage(reqDto.getPageNum(), reqDto.getPageSize());
        List<Doc> docList = docMapper.selectByExample(docExample);

        PageInfo<Doc> pageInfo = new PageInfo<>(docList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        // 列表复制
        List<DocQueryRespDto> list = CopyUtil.copyList(docList, DocQueryRespDto.class);

        PageRespDto<DocQueryRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setList(list);

        return pageRespDto;
    }

    /**
     * 根据 DocSaveReqDto 来保存一个 doc 记录，若 id 为空则add，不为空则更新
     */
    @Transactional
    public void save(DocSaveReqDto reqDto) {
        Doc docRecord = CopyUtil.copy(reqDto, Doc.class);
        Content docContent = CopyUtil.copy(reqDto, Content.class);
        if (ObjectUtils.isEmpty(docRecord.getId())) {  // 判断 id 是否为空
            // add
            Long docId = snowFlake.nextId();
            docRecord.setId(docId);
            docRecord.setViewCount(0);
            docRecord.setVoteCount(0);
            docMapper.insertSelective(docRecord);

            docContent.setId(docId);
            contentMapper.insertSelective(docContent);
        } else {
            // 更新
            docMapper.updateByPrimaryKey(docRecord);
            int count = contentMapper.updateByPrimaryKeyWithBLOBs(docContent);  // 带大字段的更新
            if (count == 0) {
                contentMapper.insert(docContent);
            }
        }
    }

    /**
     * 根据给定的文档 id 列表delete所有的文档
     * @param idList 由所要delete的文档 id 组成的列表
     */
    public void deleteDocs(List<Long> idList) {
        DocExample docExample = new DocExample();
        DocExample.Criteria criteria = docExample.createCriteria();
        criteria.andIdIn(idList);
        docMapper.deleteByExample(docExample);
    }

    /**
     * 读取一篇文档的内容
     * @param docId 所要读取的文档的id
     * @return 该文档的内容
     */
    public String readDocContent(Long docId) {
        Content content = contentMapper.selectByPrimaryKey(docId);
        docMapperCustom.increaseViewCount(docId);  // 文档阅读数 + 1
        if (ObjectUtils.isEmpty(content))
            return "";
        return content.getContent();
    }

    /**
     * 为某一个文档进行点赞
     * @param docId 文档的 id
     */
    public void vote(Long docId) {
        String ip = RequestContext.getRemoteAddr();
        String ipKey = constructIpKeyInRedis(docId, ip);
        if (redisUtil.validateRepeatedKey(ipKey, 5000)) {
            docMapperCustom.increaseVoteCount(docId);
        } else {
            throw new BusinessException(BusinessExceptionCode.VOTE_REPEAT);
        }
        // 向 ws 推送消息
        Doc docInDb = docMapper.selectByPrimaryKey(docId);
        String logId = MDC.get("LOG_ID");
        // wsService.sendInfo("【" + docInDb.getName() + "】被点赞！", logId);
        rocketMQTemplate.convertAndSend("VOTE_TOPIC", "【" + docInDb.getName() + "】被点赞！");
    }

    /**
     * 构造 IP + docID 作为存放于 redis 中的 key，24小时内不能重复
     * @return 构造生成的 key
     */
    private String constructIpKeyInRedis(Long docId, String ip) {
        return "ODC_VOTE_" + docId + "_" + ip;
    }

    /**
     * 更新所有 Ebook 的阅读量、点赞量信息
     */
    public void updateEbookFooter() {
        docMapperCustom.updateEbookFooter();
    }

}
