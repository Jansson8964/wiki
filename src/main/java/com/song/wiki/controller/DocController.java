package com.song.wiki.controller;

import com.song.wiki.dto.req.DocDeleteReqDto;
import com.song.wiki.dto.req.DocQueryReqDto;
import com.song.wiki.dto.req.DocSaveReqDto;
import com.song.wiki.dto.resp.DocQueryRespDto;
import com.song.wiki.dto.resp.ErrorCode;
import com.song.wiki.dto.resp.PageRespDto;
import com.song.wiki.dto.resp.RestfulModel;
import com.song.wiki.service.DocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Api("文档管理")
@RestController
@RequestMapping("/doc")
public class DocController {

    @Resource
    private DocService docService;


    @GetMapping("/query/{ebookId}")
    @ApiOperation(value = "获取属于某个 ebook 的全部 doc ")
    public RestfulModel<List<DocQueryRespDto>> queryDocs(@PathVariable Long ebookId) {
        List<DocQueryRespDto> docList = docService.queryDocs(ebookId);
        return new RestfulModel<>(ErrorCode.SUCCESS, "", docList);
    }

    /**
     * 对 doc 进行query的接口
     * @param docQueryReqDto query条件的参数
     * @return query到的所有doc
     */
    @GetMapping("/query")
    public RestfulModel<PageRespDto<DocQueryRespDto>> queryDocs(@Valid DocQueryReqDto docQueryReqDto) {
        PageRespDto<DocQueryRespDto> bookList = docService.queryDocs(docQueryReqDto);
        return new RestfulModel<>(ErrorCode.SUCCESS, "", bookList);
    }


    @PostMapping("/save")
    @ApiOperation(value = "保存一个 doc",
            notes = "若id非空则为更新，否则为add")
    public RestfulModel<Integer> saveDoc(@RequestBody @Valid DocSaveReqDto docSaveReqDto) {
        docService.save(docSaveReqDto);
        return new RestfulModel<>(ErrorCode.SUCCESS, "", 0);
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "delete一个 doc")
    public RestfulModel<Integer> deleteDoc(@RequestBody @Valid DocDeleteReqDto docDeleteReqDto) {
        docService.deleteDocs(docDeleteReqDto.getIds());
        return new RestfulModel<>(ErrorCode.SUCCESS, "", 0);
    }

    @GetMapping("/read-content/{docId}")
    @ApiOperation(value = "读取文档的内容", notes = "同时会对该文档的阅读数 + 1")
    @ApiParam(name = "docId", value = "文档的id", required = true)
    public RestfulModel<String> readDocContent(@PathVariable Long docId) {
        String docContent = docService.readDocContent(docId);
        return new RestfulModel<>(0, "", docContent);
    }

    @GetMapping("/vote/{docId}")
    @ApiOperation(value = "为一个doc点赞")
    @ApiParam(name = "docId", value = "文档的id", required = true)
    public RestfulModel<Integer> voteDoc(@PathVariable Long docId) {
        docService.vote(docId);
        return new RestfulModel<>(0, "", 0);
    }
}
