#import "JAPMacOSXLib.h"

static JavaVM *jvm;
static jmethodID dockMenuCallback_mID;
static jmethodID showDockMenu_mID;
static jclass jDelegateClass;

jint GetJNIEnv(JNIEnv **env, bool *mustDetach) 
{
	jint getEnvErr = JNI_OK;
	*mustDetach = false;
	if (jvm) {
		getEnvErr = (*jvm)->GetEnv(jvm, (void **)env, JNI_VERSION_1_4);
		if (getEnvErr == JNI_EDETACHED) 
		{
			getEnvErr = (*jvm)->AttachCurrentThread(jvm, (void **)env, NULL);
			if (getEnvErr == JNI_OK) 
			{
				*mustDetach = true;
			}
		}
	}
	return getEnvErr;
}

@implementation AppDelegate
- (NSMenu *) applicationDockMenu:(NSApplication *)sender
{
	JNIEnv *env = NULL;
	bool detach = false;
	
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return NULL;
	}
	
	jobject obj = (*env)->CallStaticObjectMethod(env, jDelegateClass, showDockMenu_mID);
	
	NSMenu* menu = getNSMenuFromJMenu(env, obj);
	
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
	
	return menu;
}

- (void) dockMenuCallback:(id)sender
{
	JNIEnv *env = NULL;
	bool detach = false;
	
	if(GetJNIEnv(&env, &detach) != JNI_OK)
	{
		NSLog(@"dockMenuCallback: could not attack to JVM");
		return;
	}
	
	(*env)->CallStaticVoidMethod(env, jDelegateClass, dockMenuCallback_mID);
	
	if(detach)
	{
		(*jvm)->DetachCurrentThread(jvm);
	}
}
@end

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) 
{
	jvm = vm;
	
	return JNI_VERSION_1_4;
}

//	% javap -classpath JavaHeaders.jar -s gui.JAPMacOSXLib
JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInit
  (JNIEnv * env, jclass clazz) 
{
	jDelegateClass = (*env)->NewGlobalRef(env, clazz);
	dockMenuCallback_mID = (*env)->GetStaticMethodID(env, clazz, "dockMenuCallback", "()V");
	showDockMenu_mID = (*env)->GetStaticMethodID(env, clazz, "showDockMenu", "()Ljavax/swing/JMenu;");
}

JNIEXPORT void JNICALL Java_gui_JAPMacOSXLib_nativeInitDockMenu
  (JNIEnv * env, jclass class)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
  	[NSApplication sharedApplication];
	
	[NSApp setDelegate: [AppDelegate new]];
	
	[pool release];
}

NSMenu* getNSMenuFromJMenu(JNIEnv* env, jobject obj)
{
	NSMenu* menu = [NSMenu new];
	[menu retain];
	
	jclass jMenuClass = (*env)->FindClass(env, "javax/swing/JMenu");
	jclass jMenuItemClass = (*env)->FindClass(env, "javax/swing/JMenuItem");
	jclass jSeparatorClass = (*env)->FindClass(env, "javax/swing/JSeparator");
	
	if(!(*env)->IsInstanceOf(env, obj, jMenuClass) == JNI_TRUE)
	{
		NSLog(@"getNSMenuFromJMenu: object is not a JMenu");
		return NULL;
	}
	
	jmethodID getMenuComponentCount_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponentCount", "()I");
	jmethodID getMenuComponent_mID = (*env)->GetMethodID(env, jMenuClass, "getMenuComponent", "(I)Ljava/awt/Component;");
	jmethodID getText_mID = (*env)->GetMethodID(env, jMenuItemClass, "getText", "()Ljava/lang/String;");
	
	if(getMenuComponentCount_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenu_getMenuComponentCount");
		return NULL;
	}
	
	if(getMenuComponent_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method JMenu_getMenuComponent");
		return NULL;
	}

	if(getText_mID == 0)
	{
		NSLog(@"getNSMenuFromJMenu: could not find method AbstractButton_getText");
		return NULL;
	}

	jint count = (*env)->CallIntMethod(env, obj, getMenuComponentCount_mID);
	jint i;
	
	for(i = 0; i < count; i++)
	{
		jobject item = (*env)->CallObjectMethod(env, obj, getMenuComponent_mID, i);
		
		if(item == NULL)
		{
			NSLog(@"getNSMenuFromJMenu: unknown error. skipping menu entry %i", i);
			continue;
		}
		
		if((*env)->IsInstanceOf(env, item, jMenuItemClass) == JNI_TRUE)
		{
			jstring text = (jstring) (*env)->CallObjectMethod(env, item, getText_mID);
			const char* buffer = (*env)->GetStringUTFChars(env, text, NULL);
			NSString* string = [NSString stringWithUTF8String: buffer];
			
			if((*env)->IsInstanceOf(env, item, jMenuClass) == JNI_TRUE)
			{
				NSLog(@"getNSMenuFromJMenu: adding submenu");
				NSMenu* submenu = getNSMenuFromJMenu(env, item);
				[menu setSubmenu: submenu forItem: [menu addItemWithTitle: string action:@selector(dockMenuCallback:) keyEquivalent:@""]]; 
			}
			else
			{
				NSLog(@"getNSMenuFromJMenu: adding menu item '%s'", buffer);
				[menu addItemWithTitle: string action: @selector(dockMenuCallback:) keyEquivalent: @""];
			}
			
			(*env)->ReleaseStringUTFChars(env, text, buffer);
		}
		else if((*env)->IsInstanceOf(env, item, jSeparatorClass) == JNI_TRUE)
		{
			NSLog(@"getNSMenuFromJMenu: adding separator");
			[menu addItem:[NSMenuItem separatorItem]];
		}
	}
	
	return menu;
}