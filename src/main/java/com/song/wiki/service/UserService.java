package com.song.wiki.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.song.wiki.domain.User;
import com.song.wiki.domain.UserExample;
import com.song.wiki.dto.req.UserLoginReqDto;
import com.song.wiki.dto.req.UserQueryReqDto;
import com.song.wiki.dto.req.UserResetPwdReqDto;
import com.song.wiki.dto.req.UserSaveReqDto;
import com.song.wiki.dto.resp.PageRespDto;
import com.song.wiki.dto.resp.UserLoginRespDto;
import com.song.wiki.dto.resp.UserQueryRespDto;
import com.song.wiki.exception.BusinessException;
import com.song.wiki.exception.BusinessExceptionCode;
import com.song.wiki.mapper.UserMapper;
import com.song.wiki.utils.CopyUtil;
import com.song.wiki.utils.SnowFlake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private SnowFlake snowFlake;

    public PageRespDto<UserQueryRespDto> list(UserQueryReqDto req) {
        UserExample userExample = new UserExample();
        UserExample.Criteria criteria = userExample.createCriteria();
        if (!ObjectUtils.isEmpty(req.getLoginName())) {
            criteria.andLoginNameEqualTo(req.getLoginName());
        }
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<User> userList = userMapper.selectByExample(userExample);

        PageInfo<User> pageInfo = new PageInfo<>(userList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        // 列表复制
        List<UserQueryRespDto> list = CopyUtil.copyList(userList, UserQueryRespDto.class);

        PageRespDto<UserQueryRespDto> pageRespDto = new PageRespDto<>();
        pageRespDto.setTotal(pageInfo.getTotal());
        pageRespDto.setList(list);

        return pageRespDto;
    }

    /**
     * 保存一个用户
     */
    public void save(UserSaveReqDto req) {
        User user = CopyUtil.copy(req, User.class);
        if (ObjectUtils.isEmpty(req.getId())) {
            User userInDb = selectByLoginName(req.getLoginName());  // 存于数据库中的用户信息
            if (ObjectUtils.isEmpty(userInDb)) {  // add之前判断一下 LoginName 是否重复
                // add
                user.setId(snowFlake.nextId());
                userMapper.insert(user);
            } else {
                // 用户名已存在
                throw new BusinessException(BusinessExceptionCode.USER_LOGIN_NAME_EXIST);
            }
        } else {
            // 更新
            user.setLoginName(null);  // 设置为空使得接下来对 user 的更新不再更新 LoginName 字段
            user.setPassword(null);
            userMapper.updateByPrimaryKeySelective(user);
        }
    }

    public void delete(Long id) {
        userMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据用户的 LoginName 来获取该用户的信息
     * @param LoginName 所要query的用户的 LoginName
     * @return 含有该用户信息的 User 实体对象，若query不到则返回 null
     */
    public User selectByLoginName(String LoginName) {
        UserExample userExample = new UserExample();
        UserExample.Criteria criteria = userExample.createCriteria();
        criteria.andLoginNameEqualTo(LoginName);
        List<User> userList = userMapper.selectByExample(userExample);
        if (CollectionUtils.isEmpty(userList)) {
            return null;
        } else {
            return userList.get(0);
        }
    }

    public void resetPwd(UserResetPwdReqDto reqDto) {
        User user = CopyUtil.copy(reqDto, User.class);
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     * 对用户登录进行校验
     * @param reqDto 用户登录请求的信息
     * @return 若登录成功，则返回该用户的信息；失败则抛出 BusinessException
     */
    public UserLoginRespDto login(UserLoginReqDto reqDto) {
        User userInDb = selectByLoginName(reqDto.getLoginName());
        if (ObjectUtils.isEmpty(userInDb)) {
            // 用户名不存在
            LOG.info("用户名不存在，{}", reqDto.getLoginName());
            throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
        } else {
            if (userInDb.getPassword().equals(reqDto.getPassword())) {
                // 登录成功
                UserLoginRespDto userLoginDto = CopyUtil.copy(userInDb, UserLoginRespDto.class);
                return userLoginDto;
            } else {
                // 密码不对
                LOG.info("密码不对，输入密码：{}, 数据库密码：{}", reqDto.getPassword(), userInDb.getPassword());
                throw new BusinessException(BusinessExceptionCode.LOGIN_USER_ERROR);
            }
        }
    }
}
