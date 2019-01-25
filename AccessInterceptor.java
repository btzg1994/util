package me.ffs.www.control.interceptor;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.gserv.serv.commons.util.IpUtil;

import me.ffs.www.service.PublicService;

/**
 * 接口访问拦截器
 * 
 * 控制访问频率
 * @author btzg
 * @time 2018年11月3日 上午10:27:01
 */
@Component
public class AccessInterceptor implements HandlerInterceptor{
	
	private static final Logger logger = LoggerFactory.getLogger(AccessInterceptor.class);
	
	private static Integer maxCount;
	
	@Value("${access-count-per-hour}")
	public  void setMaxCount(Integer maxCount) {
		AccessInterceptor.maxCount = maxCount;
	}

	@Override
	public synchronized boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		/*记录明细*/
		String remoteIp = IpUtil.getRemoteIp(request);
		String accessUrl = request.getRequestURL().toString();

		/*控制频率*/
		// 获取ipMap
		ServletContext app = request.getServletContext();
		Object obj = app.getAttribute(PublicService.IP_MAP_SERVLET_CONTEXT);
		HashMap<String, Integer> ipMap = null;
		if(obj == null){//第一次访问时创建新map
			ipMap = new HashMap<String, Integer>();
			app.setAttribute(PublicService.IP_MAP_SERVLET_CONTEXT, ipMap);
		}else{
			ipMap = (HashMap<String, Integer>) obj;
		}
		// 获取ip的访问次数
		Integer accessCount = ipMap.get(remoteIp);
		if(accessCount == null){//该ip第一次访问，访问次数置为0
			accessCount = 0;
		}
		logger.info("[{}]:accessCount = {}",remoteIp, accessCount);
		//判断次数是否超限
		ipMap.put(remoteIp, accessCount + 1);
		if(accessCount >= maxCount){
			logger.warn("访问频率超限，IP[{}]访问[{}]",remoteIp,accessUrl);
			response.sendRedirect(request.getContextPath() + "/global/404");
			return false;
		}
		return true;
	}
}
