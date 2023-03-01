package wang.jilijili.music.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.server.PathParam;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wang.jilijili.music.mapper.UserMapper;
import wang.jilijili.music.pojo.convert.UserConvert;
import wang.jilijili.music.pojo.dto.Code2SessionDto;
import wang.jilijili.music.pojo.dto.CreateWeChatTokenDto;
import wang.jilijili.music.pojo.entity.User;
import wang.jilijili.music.pojo.vo.Result;
import wang.jilijili.music.service.UserService;
import wang.jilijili.music.service.WeChatMPService;

/**
 * @author Amani
 * @date 2023年02月21日 17:04
 */
@RestController
@RequestMapping("/weChat")
@Tag(name = "微信小程序控制器")
public class WeixinController extends BaseController<User, UserMapper, UserService> {


    WxMaService wxMaService;
    WeChatMPService weChatMPService;
    UserConvert userConvert;

    PasswordEncoder passwordEncoder;

    public WeixinController(UserMapper mapper, UserService service,
                            WxMaService wxMaService, WeChatMPService weChatMPService,
                            PasswordEncoder passwordEncoder, UserConvert userConvert) {
        super(mapper, service);
        this.wxMaService = wxMaService;
        this.weChatMPService = weChatMPService;
        this.userConvert = userConvert;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @param redirectUrl 重定向地址
     * @return null
     * @author Amani
     * @date 2023/2/25 10:04
     */
    @GetMapping("/auth_url")
    public String getAuthUrl(@PathParam("redirectUrl") String redirectUrl) {
        return redirectUrl;
    }

    /**
     * 获取小程序openId等信息
     *
     * @param code2SessionDto code 和账号密码
     * @return 返回用户信息
     * @throws WxErrorException
     */
    @PostMapping("/")
    public Result<?> authUrl(@Validated @RequestBody Code2SessionDto code2SessionDto) throws WxErrorException {
        WxMaJscode2SessionResult sessionResult = wxMaService.jsCode2SessionInfo(code2SessionDto.getCode());
        return Result.ok(sessionResult);
    }


    @PostMapping("/createToken")
    public Result<?> createToken(@Validated @RequestBody
                                 CreateWeChatTokenDto createWeChatTokenDto) {

        String token = this.createToken(createWeChatTokenDto, this.passwordEncoder);
        return Result.ok(token);
    }
}
