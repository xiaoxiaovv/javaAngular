package com.istar.mediabroken.api.account

import com.istar.mediabroken.api.CurrentUserId
import com.istar.mediabroken.entity.account.Menu
import com.istar.mediabroken.service.account.MenuService
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

import static com.istar.mediabroken.api.ApiResult.apiResult

/**
 * Author: hh
 * Time: 2018/1/26
 */
@RestController
@Slf4j
@RequestMapping(value = "/api/account/menu")
class MenuApiController {
    @Autowired
    private MenuService menuService

    /**
     * 获取用户的菜单
     * @param userId
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Map getUserMenu(
            HttpServletRequest request,
            @CurrentUserId Long userId,
            @RequestParam(value = "flag", required = false, defaultValue = "") String flag
    ) {
        def menuMap = menuService.getMenuByUserId(request, userId)
        String serverName = request.getServerName()
        if (serverName in ["critest.zhihuibian.com", "zncb.zhihuibian.com"]) {
            //获取右侧个人中心菜单
            List<Menu> rightMenu = menuMap.get(2)
            for (Iterator<Menu> item = rightMenu.iterator(); item.hasNext();) {
                Menu menu = item.next();
                if ("同步设置".equals(menu.getName())) {
                    item.remove()
                } else if ("推送历史".equals(menu.getName())) {
                    item.remove()
                } else if ("站点设置".equals(menu.getName())) {
                    item.remove()
                } else if ("主题设置".equals(menu.getName())) {
                    item.remove()
                } else if ("首页设置".equals(menu.getName())) {
                    item.remove()
                }
            }
            menuMap.put(2, rightMenu)
            //获取左侧菜单
            List<Menu> leftMenu = menuMap.get(1)
            if (flag) {
                List<Menu> newMenu = getMenuList(leftMenu, flag)
                menuMap.put(1, newMenu)
            }
        }
        return apiResult([status: HttpStatus.SC_OK, msg: menuMap])
    }

    List<Menu> getMenuList(List<Menu> oldMenuList, String flag) {
        List<Menu> menuList = new ArrayList<>()
        switch (flag) {
            case "1": //宣传督导
                menuList.add(getMenuByName(oldMenuList, "传播测评", 0))
                break
            case "2"://智能采编
                menuList.add(getMenuByName(oldMenuList, "自动采编", 0))
                menuList.add(getMenuByName(oldMenuList, "智能组稿", 0))
                menuList.add(getMenuByName(oldMenuList, "版权监控", 1))
                break
            case "3"://媒资内容
/*                Menu menu = getMenuByName(oldMenuList, "自动采编")
                List<Menu> subList = menu.getSubList()
                Menu menuSecond = getMenuByName(subList, "最新资讯")
                subList.clear()
                subList.add(menuSecond)
                menuList.add(menu)*/
                break
            case "4"://网站设置
                break
            case "5"://权限管理
                menuList.add(getMenuByName(oldMenuList, "团队管理", 0))
                break
        }
        return menuList
    }

    Menu getMenuByName(List<Menu> menuList, String menuName, Integer isLeaf) {
        for (Menu menu : menuList) {
            if ((menu.getName().equals(menuName)) && (menu.getIsLeaf() == isLeaf)) {
                return menu
            }
        }
    }

}
