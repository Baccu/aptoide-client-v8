/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 08/07/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid;

import android.content.Context;

import java.io.File;

import cm.aptoide.pt.database.realm.FileToDownload;
import cm.aptoide.pt.database.realm.Update;
import cm.aptoide.pt.model.v7.Type;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.install.InstallManager;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by neuro on 17-05-2016.
 */
@AllArgsConstructor
public class UpdateDisplayable extends Displayable {

	@Getter private String packageName;
	@Getter private long appId;
	@Getter private String label;
	@Getter private String icon;
	@Getter private String md5;
	@Getter private String apkPath;
	@Getter private String alternativeApkPath;
	@Getter private String updateVersionName;

	// Obb
	@Getter private String mainObbPath;
	@Getter private String mainObbMd5;
	@Getter private String patchObbPath;
	@Getter private String patchObbMd5;

	private InstallManager installManager;

	public UpdateDisplayable() {
	}

	public static UpdateDisplayable create(Update update, InstallManager installManager) {
		return new UpdateDisplayable(update.getPackageName(),update.getAppId(), update.getLabel(), update.getIcon(), update.getMd5(), update.getApkPath(),
				update.getAlternativeApkPath(), update.getUpdateVersionName(), update.getMainObbPath(), update.getMainObbMd5(), update.getPatchObbPath(),
				update.getPatchObbMd5(), installManager);
	}

	public void install(Context context, FileToDownload file) {
		installManager.install(context, new File(file.getFilePath()));
	}

	@Override
	public Type getType() {
		return Type.UPDATE;
	}

	@Override
	public int getViewLayout() {
		return R.layout.update_row;
	}
}
