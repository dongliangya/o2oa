package com.x.cms.assemble.control.jaxrs.appinfo;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.base.core.project.tools.SortTools;
import com.x.cms.core.entity.AppInfo;
import com.x.cms.core.entity.CategoryInfo;

import net.sf.ehcache.Element;

public class ActionListWhatICanManage_WithAppType extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionListWhatICanManage_WithAppType.class);

	@SuppressWarnings("unchecked")
	protected ActionResult<List<Wo>> execute( HttpServletRequest request, EffectivePerson effectivePerson, String appType ) throws Exception {
		ActionResult<List<Wo>> result = new ActionResult<>();
		List<Wo> wos = null;
		List<AppInfo> appInfoList = null;
		List<CategoryInfo> catacoryList = null;
		List<WoCategory> wrapOutCatacoryList = null;
		List<String> app_ids = null;
		Boolean isXAdmin = false;
		Boolean check = true;
		String personName = effectivePerson.getDistinguishedName();
		
		try {
			isXAdmin = userManagerService.isManager( effectivePerson );
		} catch (Exception e) {
			check = false;
			Exception exception = new ExceptionAppInfoProcess(e, "系统在检查用户是否是平台管理员时发生异常。Name:" + personName);
			result.error(exception);
			logger.error(e, effectivePerson, request, null);
		}
		String cacheKey = ApplicationCache.concreteCacheKey(personName, "manage", "appType", appType,  isXAdmin);
		Element element = cache.get(cacheKey);

		if ((null != element) && (null != element.getObjectValue())) {
			wos = (List<Wo>) element.getObjectValue();
			result.setData(wos);
		} else {
			if (check) {
				if (isXAdmin) {
					try {
						appInfoList = appInfoServiceAdv.listAll( appType, "全部");
					} catch (Exception e) {
						check = false;
						Exception exception = new ExceptionAppInfoProcess(e, "查询所有应用栏目信息对象时发生异常");
						result.error(exception);
						logger.error(e, effectivePerson, request, null);
					}
				} else {
					try {
						List<String> unitNames = userManagerService.listUnitNamesWithPerson( personName );
						List<String> groupNames = userManagerService.listGroupNamesByPerson( personName );
						app_ids = permissionQueryService.listManageableAppIdsByPerson( personName, unitNames, groupNames, appType, "全部", 1000 );
						if (app_ids != null && !app_ids.isEmpty()) {
							try {
								appInfoList = appInfoServiceAdv.list( app_ids );
							} catch (Exception e) {
								check = false;
								Exception exception = new ExceptionAppInfoProcess(e, "系统根据ID列表查询应用栏目信息对象时发生异常。");
								result.error(exception);
								logger.error(e, effectivePerson, request, null);
							}
						}
					} catch (Exception e) {
						check = false;
						Exception exception = new ExceptionAppInfoProcess(e,
								"系统在根据用户权限查询所有管理的栏目信息时发生异常。Name:" + personName);
						result.error(exception);
						logger.error(e, effectivePerson, request, null);
					}
				}
			}
			if (check) {
				if (appInfoList != null && !appInfoList.isEmpty()) {
					try {
						wos = Wo.copier.copy(appInfoList);
						SortTools.asc( wos, "appInfoSeq");
						cache.put(new Element(cacheKey, wos));
						result.setData(wos);
					} catch (Exception e) {
						check = false;
						Exception exception = new ExceptionAppInfoProcess(e, "将查询出来的应用栏目信息对象转换为可输出的数据信息时发生异常。");
						result.error(exception);
						logger.error(e, effectivePerson, request, null);
					}
				}
			}
			if (check) {
				if ( wos != null && wos.size() > 0) {
					List<String> query_categoryList = null;
					for ( Wo wo : wos ) {
						query_categoryList = new ArrayList<>();				
						if( ListTools.isNotEmpty( wo.getCategoryList() )) {
							for( String categoryId : wo.getCategoryList() ) {
								if( categoryId != null && !"null".equalsIgnoreCase( categoryId.trim() ) ) {
									query_categoryList.add( categoryId );
								}
							}
						}
						try {
							catacoryList = categoryInfoServiceAdv.list( query_categoryList );
							if ( catacoryList != null && !catacoryList.isEmpty() ) {
								try {
									wrapOutCatacoryList = WoCategory.copier.copy( catacoryList );
									wo.setWrapOutCategoryList( wrapOutCatacoryList );
								} catch (Exception e) {
									check = false;
									Exception exception = new ExceptionAppInfoProcess(e, "将查询出来的分类信息对象转换为可输出的数据信息时发生异常。");
									result.error(exception);
									logger.error(e, effectivePerson, request, null);
								}
							}
						} catch (Exception e) {
							check = false;
							Exception exception = new ExceptionAppInfoProcess(e, "系统根据ID列表查询分类信息对象时发生异常。");
							result.error(exception);
							logger.error(e, effectivePerson, request, null);
						}
					}
				}
			}
		}
		return result;
	}
}