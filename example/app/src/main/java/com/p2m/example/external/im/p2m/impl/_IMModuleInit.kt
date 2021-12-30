package com.p2m.example.external.im.p2m.impl

import com.p2m.core.module.ModuleInit
import com.p2m.example.external.im.IMModuleInit

public class _IMModuleInit(
  moduleInitReal: ModuleInit = IMModuleInit()
) : com.p2m.example.external.im.p2m.api.IMModuleInit, ModuleInit by moduleInitReal
