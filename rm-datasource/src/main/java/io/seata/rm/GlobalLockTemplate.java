/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm;

import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.context.RootContext;
import io.seata.core.model.GlobalLockConfig;

/**
 * executor template for local transaction which need global lock
 * @author selfishlover
 * //全局锁模板
 */
public class GlobalLockTemplate {

    public Object execute(GlobalLockExecutor executor) throws Throwable {
        //首先获取全局锁。没有锁住的话就上锁
        boolean alreadyInGlobalLock = RootContext.requireGlobalLock();
        if (!alreadyInGlobalLock) {
            RootContext.bindGlobalLockFlag();
        }

        // set my config to config holder so that it can be access in further execution
        // for example, LockRetryController can access it with config holder
        GlobalLockConfig myConfig = executor.getGlobalLockConfig();
        //把配置设置到线程本地中
        GlobalLockConfig previousConfig = GlobalLockConfigHolder.setAndReturnPrevious(myConfig);

        try {
            return executor.execute();
        } finally {
            // only unbind when this is the root caller.
            // otherwise, the outer caller would lose global lock flag
            //根据当前调用者之前是未锁定状态的才进行解锁。而不是根据RootContent中重新获取的锁定标志
            if (!alreadyInGlobalLock) {
                RootContext.unbindGlobalLockFlag();
            }

            // if previous config is not null, we need to set it back
            // so that the outer logic can still use their config
            //老的配置设置回去
            if (previousConfig != null) {
                GlobalLockConfigHolder.setAndReturnPrevious(previousConfig);
            } else {
                GlobalLockConfigHolder.remove();
            }
        }
    }
}
