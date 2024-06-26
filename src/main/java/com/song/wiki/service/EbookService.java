package com.song.wiki.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.song.wiki.domain.Ebook;
import com.song.wiki.domain.EbookExample;
import com.song.wiki.dto.req.EbookQueryReqDto;
import com.song.wiki.dto.req.EbookSaveReqDto;
import com.song.wiki.dto.resp.EbookQueryRespDto;
import com.song.wiki.dto.resp.PageRespDto;
import com.song.wiki.mapper.EbookMapper;
import com.song.wiki.utils.CopyUtil;
import com.song.wiki.utils.SnowFlake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class EbookService {

    private static final Logger LOG = LoggerFactory.getLogger(EbookService.class);

    @Resource
    private EbookMapper ebookMapper;

    @Resource
    private SnowFlake snowFlake;


    /**
     * 根据query条件对数据库中的 ebook 进行query并返回query到的 ebook
     */
    public PageRespDto<EbookQueryRespDto> queryEbooks(EbookQueryReqDto reqDto) {
        EbookExample ebookExample = new EbookExample();
        EbookExample.Criteria criteria = ebookExample.createCriteria();
        if (!ObjectUtils.isEmpty(reqDto.getName())) {
            criteria.andNameLike("%" + reqDto.getName() + "%");
        }
        if (!ObjectUtils.isEmpty(reqDto.getCategoryId2())) {
            criteria.andCategory2IdEqualTo(reqDto.getCategoryId2());
        }
        PageHelper.startPage(reqDto.getPageNum(), reqDto.getPageSize());
        List<Ebook> ebookList = ebookMapper.selectByExample(ebookExample);

        PageInfo<Ebook> pageInfo = new PageInfo<>(ebookList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        // 列表复制
        List<EbookQueryRespDto> list = CopyUtil.copyList(ebookList, EbookQueryRespDto.class);

        PageRespDto<EbookQueryRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setList(list);

        return pageRespDto;
    }

    /**
     * 根据 EbookSaveReqDto 来保存一个 ebook 记录，若 id 为空则add，不为空则更新
     */
    public void save(EbookSaveReqDto reqDto) {
        Ebook ebookRecord = CopyUtil.copy(reqDto, Ebook.class);
        if (ObjectUtils.isEmpty(ebookRecord.getId())) {  // 判断 id 是否为空
            ebookRecord.setId(snowFlake.nextId());
            ebookMapper.insertSelective(ebookRecord);
        } else {
            ebookMapper.updateByPrimaryKey(ebookRecord);
        }
    }

    public void deleteOneEbook(Long ebookId) {
        ebookMapper.deleteByPrimaryKey(ebookId);
    }
}
